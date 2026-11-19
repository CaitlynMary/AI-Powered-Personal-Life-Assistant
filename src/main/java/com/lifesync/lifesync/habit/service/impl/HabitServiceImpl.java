package com.lifesync.lifesync.habit.service.impl;

import com.lifesync.lifesync.exception.BadRequestException;
import com.lifesync.lifesync.exception.DuplicateResourceException;
import com.lifesync.lifesync.exception.ResourceNotFoundException;
import com.lifesync.lifesync.habit.dto.*;
import com.lifesync.lifesync.habit.entity.*;
import com.lifesync.lifesync.habit.repository.HabitLogRepository;
import com.lifesync.lifesync.habit.repository.HabitRepository;
import com.lifesync.lifesync.habit.service.HabitService;
import com.lifesync.lifesync.notification.service.NotificationEventService;
import com.lifesync.lifesync.user.entity.User;
import com.lifesync.lifesync.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HabitServiceImpl implements HabitService {
    private final HabitRepository habits;
    private final HabitLogRepository logs;
    private final UserRepository users;
    private final NotificationEventService notificationEvents;
    public HabitServiceImpl(HabitRepository habits,HabitLogRepository logs,UserRepository users){this(habits,logs,users,null);}
    @org.springframework.beans.factory.annotation.Autowired
    public HabitServiceImpl(HabitRepository habits,HabitLogRepository logs,UserRepository users,NotificationEventService notificationEvents){this.habits=habits;this.logs=logs;this.users=users;this.notificationEvents=notificationEvents;}

    @Override @Transactional
    public HabitResponse create(Long userId,HabitRequest request){
        validateHabit(request);User user=users.findById(userId).orElseThrow(()->new ResourceNotFoundException("User account was not found"));
        Habit habit=new Habit();habit.setUser(user);apply(habit,request);habit.setActive(true);return response(habits.save(habit));
    }
    @Override @Transactional(readOnly=true)
    public List<HabitResponse> getAll(Long userId){return habits.findAllByUser_IdOrderByNameAsc(userId).stream().map(this::response).toList();}
    @Override @Transactional(readOnly=true)
    public HabitResponse getById(Long userId,Long habitId){return response(findHabit(userId,habitId));}
    @Override @Transactional
    public HabitResponse update(Long userId,Long habitId,HabitRequest request){
        validateHabit(request);Habit habit=findHabit(userId,habitId);
        boolean wouldOrphanLog=logs.findAllByHabit_IdOrderByCompletionDateDesc(habitId).stream()
                .anyMatch(log->log.getCompletionDate().isBefore(request.getStartDate())||(request.getEndDate()!=null&&log.getCompletionDate().isAfter(request.getEndDate())));
        if(wouldOrphanLog)throw new BadRequestException("Habit date range cannot exclude existing history");
        apply(habit,request);return response(habits.save(habit));
    }
    @Override @Transactional
    public void delete(Long userId,Long habitId){
        Habit habit=findHabit(userId,habitId);logs.deleteAllByHabit_Id(habitId);habits.delete(habit);
    }
    @Override @Transactional
    public HabitResponse deactivate(Long userId,Long habitId){Habit habit=findHabit(userId,habitId);habit.setActive(false);return response(habits.save(habit));}
    @Override @Transactional
    public HabitLogResponse createLog(Long userId,Long habitId,HabitLogRequest request){
        Habit habit=findHabit(userId,habitId);requireActive(habit);validateLogDate(habit,request.getCompletionDate());
        if(logs.existsByHabit_IdAndCompletionDate(habitId,request.getCompletionDate()))throw new DuplicateResourceException("A log already exists for this habit and date");
        if(Boolean.TRUE.equals(request.getCompleted()))validateFrequencyEligibility(habit,request.getCompletionDate());
        HabitLog log=new HabitLog();log.setHabit(habit);log.setCompletionDate(request.getCompletionDate());log.setCompleted(request.getCompleted());log.setNotes(blankToNull(request.getNotes()));
        HabitLog saved;
        try{saved=logs.save(log);}catch(DataIntegrityViolationException ex){throw new DuplicateResourceException("A log already exists for this habit and date");}
        if(Boolean.TRUE.equals(saved.getCompleted())&&notificationEvents!=null)notificationEvents.habitCompleted(saved);
        return logResponse(saved);
    }
    @Override @Transactional(readOnly=true)
    public List<HabitLogResponse> getLogs(Long userId,Long habitId){findHabit(userId,habitId);return logs.findAllByHabit_IdOrderByCompletionDateDesc(habitId).stream().map(this::logResponse).toList();}
    @Override @Transactional
    public HabitLogResponse complete(Long userId,Long habitId,HabitCompletionRequest request){
        Habit habit=findHabit(userId,habitId);requireActive(habit);LocalDate date=request==null||request.getCompletionDate()==null?LocalDate.now():request.getCompletionDate();validateLogDate(habit,date);
        boolean completed=request==null||request.getCompleted()==null||request.getCompleted();
        HabitLog log=logs.findByHabit_IdAndCompletionDate(habitId,date).orElse(null);
        if(!completed&&log==null)throw new BadRequestException("There is no saved completion to undo for this habit and date");
        if(completed&&!Boolean.TRUE.equals(log==null?null:log.getCompleted()))validateFrequencyEligibility(habit,date);
        boolean newlyCompleted=completed&&!Boolean.TRUE.equals(log==null?null:log.getCompleted());
        if(log==null){log=new HabitLog();log.setHabit(habit);log.setCompletionDate(date);}
        log.setCompleted(completed);
        if(request!=null&&request.getNotes()!=null)log.setNotes(blankToNull(request.getNotes()));
        HabitLog saved;
        try{saved=logs.save(log);}catch(DataIntegrityViolationException ex){throw new DuplicateResourceException("A log already exists for this habit and date");}
        if(newlyCompleted&&notificationEvents!=null)notificationEvents.habitCompleted(saved);
        return logResponse(saved);
    }
    @Override @Transactional(readOnly=true)
    public HabitStreakResponse streak(Long userId,Long habitId){
        Habit habit=findHabit(userId,habitId);LocalDate today=LocalDate.now();Set<LocalDate> completed=completedDates(habitId,today);
        return metrics(habit,completed,today);
    }
    @Override @Transactional(readOnly=true)
    public HabitCalendarResponse calendar(Long userId,Long habitId,int year,int month){
        Habit habit=findHabit(userId,habitId);
        if(year<1||month<1||month>12)throw new BadRequestException("Calendar year or month is invalid");
        YearMonth requested=YearMonth.of(year,month);LocalDate first=requested.atDay(1);LocalDate last=requested.atEndOfMonth();LocalDate today=LocalDate.now();
        LocalDate periodStart=monday(first),periodEnd=monday(last).plusDays(6);
        if(periodEnd.isAfter(today))periodEnd=today;
        List<HabitLog> periodLogs=periodStart.isAfter(periodEnd)?List.of():logs.findAllByHabit_IdAndCompletionDateBetweenOrderByCompletionDateAsc(habitId,periodStart,periodEnd);
        Set<LocalDate> completedDates=periodLogs.stream().filter(l->Boolean.TRUE.equals(l.getCompleted())).map(HabitLog::getCompletionDate).collect(Collectors.toSet());
        Map<LocalDate,HabitLog> monthLogs=periodLogs.stream().filter(l->!l.getCompletionDate().isBefore(first)&&!l.getCompletionDate().isAfter(last))
                .collect(Collectors.toMap(HabitLog::getCompletionDate,Function.identity(),(a,b)->a));
        List<HabitCalendarDay> days=new ArrayList<>();long completed=0;
        for(LocalDate date=first;!date.isAfter(last);date=date.plusDays(1)){
            boolean inRange=inHabitRange(habit,date);HabitLog log=monthLogs.get(date);boolean done=log!=null&&Boolean.TRUE.equals(log.getCompleted());
            boolean eligible=Boolean.TRUE.equals(habit.getActive())&&inRange&&!date.isAfter(today)&&frequencyEligible(habit,date,completedDates);
            days.add(new HabitCalendarDay(date,eligible,done,log==null?null:log.getNotes()));
            if(done)completed++;
        }
        long eligibleSlots=expectedSlots(habit,first,last.isAfter(today)?today:last);
        double percentage=calendarPercentage(habit,completedDates,first,last,today);
        return new HabitCalendarResponse(habitId,year,month,completed,eligibleSlots,percentage,days);
    }
    @Override @Transactional(readOnly=true)
    public HabitStatisticsResponse statistics(Long userId){
        List<Habit> owned=habits.findAllByUser_IdOrderByNameAsc(userId);long totalCompleted=0,weekDone=0,monthDone=0;
        LocalDate today=LocalDate.now(),weekStart=monday(today),monthStart=today.withDayOfMonth(1);
        long weekExpected=0,weekSatisfied=0,monthExpected=0,monthSatisfied=0;
        List<HabitLog> allLogs=logs.findAllByHabit_User_Id(userId);
        Map<Long,Set<LocalDate>> datesByHabit=allLogs.stream().filter(l->Boolean.TRUE.equals(l.getCompleted()))
                .collect(Collectors.groupingBy(l->l.getHabit().getId(),Collectors.mapping(HabitLog::getCompletionDate,Collectors.toSet())));
        for(Habit habit:owned){
            Set<LocalDate> dates=datesByHabit.getOrDefault(habit.getId(),Set.of());
            totalCompleted+=dates.size();
            if(!Boolean.TRUE.equals(habit.getActive()))continue;
            Set<LocalDate> inWeek=dates.stream().filter(d->!d.isBefore(weekStart)&&!d.isAfter(today)).collect(Collectors.toSet());weekDone+=inWeek.size();
            Set<LocalDate> inMonth=dates.stream().filter(d->!d.isBefore(monthStart)&&!d.isAfter(today)).collect(Collectors.toSet());monthDone+=inMonth.size();
            HabitWindow window=window(habit,today);
            LocalDate ws=weekStart.isBefore(window.start)?window.start:weekStart;
            if(habit.getFrequency()!=HabitFrequency.MONTHLY&&!window.empty&& !ws.isAfter(window.end)){long expected=expectedSlots(habit,ws,today);weekExpected+=expected;weekSatisfied+=satisfiedSlots(habit,inWeek,ws,today);}
            LocalDate ms=monthStart.isBefore(window.start)?window.start:monthStart;
            if(!window.empty&&!ms.isAfter(window.end)){long expected=expectedSlots(habit,ms,today);monthExpected+=expected;monthSatisfied+=satisfiedSlots(habit,inMonth,ms,today);}
        }
        return new HabitStatisticsResponse(owned.size(),habits.countByUser_IdAndActiveTrue(userId),totalCompleted,weekDone,monthDone,percent(weekSatisfied,weekExpected),percent(monthSatisfied,monthExpected));
    }

    private HabitStreakResponse metrics(Habit habit,Set<LocalDate> completed,LocalDate today){
        HabitWindow window=window(habit,today);
        int current=switch(habit.getFrequency()){
            case DAILY->currentDaily(completed,window);
            case WEEKLY->currentWeekly(habit,completed,window);
            case MONTHLY->currentMonthly(habit,completed,window);
        };
        int longest=switch(habit.getFrequency()){
            case DAILY->longestDaily(completed,window);
            case WEEKLY->longestWeekly(habit,completed,window);
            case MONTHLY->longestMonthly(habit,completed,window);
        };
        long expected=window.empty?0:expectedSlots(habit,window.start,window.end);long satisfied=window.empty?0:satisfiedSlots(habit,completed,window.start,window.end);
        String periodUnit=switch(habit.getFrequency()){case DAILY->"days";case WEEKLY->"weeks";case MONTHLY->"months";};
        return new HabitStreakResponse(current,longest,completed.size(),percent(satisfied,expected),periodUnit);
    }
    private int currentDaily(Set<LocalDate> completed,HabitWindow window){
        if(window.empty)return 0;LocalDate ref=window.end;LocalDate cursor=completed.contains(ref)?ref:ref.minusDays(1);int count=0;
        while(!cursor.isBefore(window.start)&&completed.contains(cursor)){count++;cursor=cursor.minusDays(1);}return count;
    }
    private int longestDaily(Set<LocalDate> completed,HabitWindow window){
        if(window.empty)return 0;int best=0,run=0;LocalDate previous=null;
        for(LocalDate date:completed.stream().filter(d->!d.isBefore(window.start)&&!d.isAfter(window.end)).sorted().toList()){
            run=previous!=null&&previous.plusDays(1).equals(date)?run+1:1;best=Math.max(best,run);previous=date;
        }return best;
    }
    private int currentWeekly(Habit habit,Set<LocalDate> completed,HabitWindow window){
        if(window.empty)return 0;LocalDate refWeek=monday(window.end);LocalDate candidate=refWeek;
        boolean weekStillOpen=candidate.plusDays(6).isAfter(LocalDate.now())
                &&(habit.getEndDate()==null||habit.getEndDate().isAfter(LocalDate.now()));
        if(!weekQualifies(habit,candidate,completed,window.end)&&weekStillOpen)candidate=candidate.minusWeeks(1);
        int count=0;while(!candidate.isBefore(monday(window.start))&&weekQualifies(habit,candidate,completed,window.end)){count++;candidate=candidate.minusWeeks(1);}return count;
    }
    private int currentMonthly(Habit habit,Set<LocalDate> completed,HabitWindow window){
        if(window.empty)return 0;YearMonth candidate=YearMonth.from(window.end);
        boolean monthStillOpen=candidate.atEndOfMonth().isAfter(LocalDate.now())
                &&(habit.getEndDate()==null||habit.getEndDate().isAfter(LocalDate.now()));
        if(!monthQualifies(habit,candidate,completed,window.end)&&monthStillOpen)candidate=candidate.minusMonths(1);
        int count=0;while(!candidate.atEndOfMonth().isBefore(window.start)&&monthQualifies(habit,candidate,completed,window.end)){count++;candidate=candidate.minusMonths(1);}return count;
    }
    private int longestWeekly(Habit habit,Set<LocalDate> completed,HabitWindow window){
        if(window.empty)return 0;int best=0,run=0;LocalDate week=monday(window.start),last=monday(window.end);
        while(!week.isAfter(last)){
            if(weekQualifies(habit,week,completed,window.end)){run++;best=Math.max(best,run);}else run=0;
            week=week.plusWeeks(1);
        }return best;
    }
    private int longestMonthly(Habit habit,Set<LocalDate> completed,HabitWindow window){
        if(window.empty)return 0;int best=0,run=0;YearMonth month=YearMonth.from(window.start),last=YearMonth.from(window.end);
        while(!month.isAfter(last)){
            if(monthQualifies(habit,month,completed,window.end)){run++;best=Math.max(best,run);}else run=0;
            month=month.plusMonths(1);
        }return best;
    }
    private boolean weekQualifies(Habit habit,LocalDate week,Set<LocalDate> completed,LocalDate asOf){
        LocalDate start=week.isBefore(habit.getStartDate())?habit.getStartDate():week;
        LocalDate end=week.plusDays(6);if(habit.getEndDate()!=null&&end.isAfter(habit.getEndDate()))end=habit.getEndDate();
        if(end.isBefore(start))return false;int activeDays=(int)ChronoUnit.DAYS.between(start,end)+1;
        LocalDate activeStart=start,activeEnd=end,countEnd=end.isAfter(asOf)?asOf:end;
        int target=Math.min(habit.getTargetDaysPerWeek(),activeDays);long done=completed.stream().filter(d->!d.isBefore(activeStart)&&!d.isAfter(activeEnd)&&!d.isAfter(countEnd)).count();return done>=target;
    }
    private boolean monthQualifies(Habit habit,YearMonth month,Set<LocalDate> completed,LocalDate asOf){
        LocalDate start=month.atDay(1).isBefore(habit.getStartDate())?habit.getStartDate():month.atDay(1);
        LocalDate end=month.atEndOfMonth();if(habit.getEndDate()!=null&&end.isAfter(habit.getEndDate()))end=habit.getEndDate();if(end.isAfter(asOf))end=asOf;
        if(end.isBefore(start))return false;
        for(LocalDate date:completed)if(!date.isBefore(start)&&!date.isAfter(end))return true;
        return false;
    }
    private boolean frequencyEligible(Habit habit,LocalDate date,Set<LocalDate> completed){
        if(completed.contains(date))return true;
        if(habit.getFrequency()==HabitFrequency.DAILY)return true;
        if(habit.getFrequency()==HabitFrequency.WEEKLY){
            LocalDate weekStart=monday(date),weekEnd=weekStart.plusDays(6);
            long completedThisWeek=completed.stream().filter(d->!d.isBefore(weekStart)&&!d.isAfter(weekEnd)).count();
            return completedThisWeek<habit.getTargetDaysPerWeek();
        }
        YearMonth month=YearMonth.from(date);
        return completed.stream().noneMatch(d->YearMonth.from(d).equals(month));
    }
    private void validateFrequencyEligibility(Habit habit,LocalDate date){
        if(habit.getFrequency()==HabitFrequency.WEEKLY){
            LocalDate start=monday(date),end=start.plusDays(6);
            if(logs.countByHabit_IdAndCompletedTrueAndCompletionDateBetween(habit.getId(),start,end)>=habit.getTargetDaysPerWeek())
                throw new BadRequestException("The weekly completion target has already been met");
        }else if(habit.getFrequency()==HabitFrequency.MONTHLY){
            YearMonth month=YearMonth.from(date);
            if(logs.countByHabit_IdAndCompletedTrueAndCompletionDateBetween(habit.getId(),month.atDay(1),month.atEndOfMonth())>0)
                throw new BadRequestException("A completion has already been recorded for this calendar month");
        }
    }
    private long expectedSlots(Habit habit,LocalDate start,LocalDate end){
        HabitWindow bound=window(habit,end);LocalDate from=start.isBefore(bound.start)?bound.start:start;LocalDate to=end.isAfter(bound.end)?bound.end:end;if(bound.empty||to.isBefore(from))return 0;
        if(habit.getFrequency()==HabitFrequency.DAILY)return ChronoUnit.DAYS.between(from,to)+1;
        if(habit.getFrequency()==HabitFrequency.MONTHLY)return ChronoUnit.MONTHS.between(YearMonth.from(from),YearMonth.from(to))+1;
        long expected=0;for(LocalDate week=monday(from);!week.isAfter(to);week=week.plusWeeks(1)){
            LocalDate activeStart=week.isBefore(from)?from:week;LocalDate weekEnd=week.plusDays(6);LocalDate activeEnd=weekEnd.isAfter(to)?to:weekEnd;
            expected+=Math.min(habit.getTargetDaysPerWeek(),ChronoUnit.DAYS.between(activeStart,activeEnd)+1);
        }return expected;
    }
    private long satisfiedSlots(Habit habit,Set<LocalDate> completed,LocalDate start,LocalDate end){
        HabitWindow bound=window(habit,end);LocalDate from=start.isBefore(bound.start)?bound.start:start;LocalDate to=end.isAfter(bound.end)?bound.end:end;if(bound.empty||to.isBefore(from))return 0;
        Set<LocalDate> inRange=completed.stream().filter(d->!d.isBefore(from)&&!d.isAfter(to)).collect(Collectors.toSet());
        if(habit.getFrequency()==HabitFrequency.DAILY)return inRange.size();
        if(habit.getFrequency()==HabitFrequency.MONTHLY)return inRange.stream().map(YearMonth::from).distinct().count();
        long satisfied=0;for(LocalDate week=monday(from);!week.isAfter(to);week=week.plusWeeks(1)){
            LocalDate activeStart=week.isBefore(from)?from:week;LocalDate weekEnd=week.plusDays(6);LocalDate activeEnd=weekEnd.isAfter(to)?to:weekEnd;
            long planned=Math.min(habit.getTargetDaysPerWeek(),ChronoUnit.DAYS.between(activeStart,activeEnd)+1);
            long done=inRange.stream().filter(d->!d.isBefore(activeStart)&&!d.isAfter(activeEnd)).count();satisfied+=Math.min(planned,done);
        }return satisfied;
    }
    private double calendarPercentage(Habit habit,Set<LocalDate> completed,LocalDate start,LocalDate end,LocalDate today){
        LocalDate rangeEnd=end.isAfter(today)?today:end;
        if(rangeEnd.isBefore(start))return 0;
        return percent(satisfiedSlots(habit,completed,start,rangeEnd),expectedSlots(habit,start,rangeEnd));
    }
    private Set<LocalDate> completedDates(Long habitId,LocalDate asOf){return logs.findAllByHabit_IdAndCompletedTrueAndCompletionDateLessThanEqualOrderByCompletionDateAsc(habitId,asOf).stream().map(HabitLog::getCompletionDate).collect(Collectors.toCollection(TreeSet::new));}
    private HabitWindow window(Habit habit,LocalDate asOf){LocalDate end=habit.getEndDate()!=null&&habit.getEndDate().isBefore(asOf)?habit.getEndDate():asOf;return new HabitWindow(habit.getStartDate(),end,end.isBefore(habit.getStartDate()));}
    private boolean inHabitRange(Habit habit,LocalDate date){return !date.isBefore(habit.getStartDate())&&(habit.getEndDate()==null||!date.isAfter(habit.getEndDate()));}
    private LocalDate monday(LocalDate date){return date.with(DayOfWeek.MONDAY);}
    private double percent(long numerator,long denominator){return denominator==0?0.0:Math.round(Math.min(100.0,numerator*100.0/denominator)*100.0)/100.0;}
    private void validateHabit(HabitRequest request){
        if(request.getStartDate()==null||request.getFrequency()==null||request.getTargetDaysPerWeek()==null)throw new BadRequestException("Habit frequency, target days, and start date are required");
        if(request.getTargetDaysPerWeek()<1||request.getTargetDaysPerWeek()>7)throw new BadRequestException("targetDaysPerWeek must be between 1 and 7");
        if(request.getEndDate()!=null&&request.getEndDate().isBefore(request.getStartDate()))throw new BadRequestException("endDate cannot be before startDate");
    }
    private void validateLogDate(Habit habit,LocalDate date){
        if(date==null)throw new BadRequestException("completionDate is required");
        if(date.isAfter(LocalDate.now()))throw new BadRequestException("Habit logs cannot be recorded for a future date");
        if(date.isBefore(habit.getStartDate())||(habit.getEndDate()!=null&&date.isAfter(habit.getEndDate())))throw new BadRequestException("completionDate must be within the habit date range");
    }
    private void requireActive(Habit habit){if(!Boolean.TRUE.equals(habit.getActive()))throw new BadRequestException("Habit is deactivated and cannot be logged");}
    private Habit findHabit(Long userId,Long id){return habits.findByIdAndUser_Id(id,userId).orElseThrow(()->new ResourceNotFoundException("Habit was not found"));}
    private void apply(Habit h,HabitRequest r){h.setName(r.getName().trim());h.setDescription(blankToNull(r.getDescription()));h.setFrequency(r.getFrequency());h.setTargetDaysPerWeek(r.getTargetDaysPerWeek());h.setStartDate(r.getStartDate());h.setEndDate(r.getEndDate());}
    private String blankToNull(String v){return v==null||v.isBlank()?null:v.trim();}
    private HabitResponse response(Habit h){return new HabitResponse(h.getId(),h.getName(),h.getDescription(),h.getFrequency(),h.getTargetDaysPerWeek(),h.getStartDate(),h.getEndDate(),h.getActive(),h.getCreatedAt(),h.getUpdatedAt());}
    private HabitLogResponse logResponse(HabitLog l){return new HabitLogResponse(l.getId(),l.getHabit().getId(),l.getCompletionDate(),l.getCompleted(),l.getNotes(),l.getCreatedAt());}
    private static class HabitWindow { private final LocalDate start;private final LocalDate end;private final boolean empty;private HabitWindow(LocalDate start,LocalDate end,boolean empty){this.start=start;this.end=end;this.empty=empty;} }
}

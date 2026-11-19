package com.lifesync.lifesync.habit.service;

import com.lifesync.lifesync.exception.BadRequestException;
import com.lifesync.lifesync.exception.DuplicateResourceException;
import com.lifesync.lifesync.exception.ResourceNotFoundException;
import com.lifesync.lifesync.habit.dto.*;
import com.lifesync.lifesync.habit.entity.*;
import com.lifesync.lifesync.habit.repository.HabitLogRepository;
import com.lifesync.lifesync.habit.repository.HabitRepository;
import com.lifesync.lifesync.habit.service.impl.HabitServiceImpl;
import com.lifesync.lifesync.notification.service.NotificationEventService;
import com.lifesync.lifesync.user.entity.User;
import com.lifesync.lifesync.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HabitServiceImplTest {
    @Mock HabitRepository habits;
    @Mock HabitLogRepository logs;
    @Mock UserRepository users;
    @Mock NotificationEventService notificationEvents;
    private HabitServiceImpl service;
    private User owner;
    @BeforeEach void setUp(){service=new HabitServiceImpl(habits,logs,users,notificationEvents);owner=new User();owner.setId(12L);}

    @Test void createsGetsUpdatesAndDeletesOwnedHabit() {
        Habit habit=habit(HabitFrequency.WEEKLY,3,LocalDate.now().minusDays(4),null);
        when(users.findById(12L)).thenReturn(Optional.of(owner));when(habits.save(any(Habit.class))).thenAnswer(i->{Habit h=i.getArgument(0);h.setId(6L);return h;});
        assertEquals(6L,service.create(12L,request(HabitFrequency.WEEKLY,3,LocalDate.now().minusDays(4),null)).getId());
        when(habits.findByIdAndUser_Id(6L,12L)).thenReturn(Optional.of(habit));
        when(logs.findAllByHabit_IdOrderByCompletionDateDesc(6L)).thenReturn(List.of());
        assertEquals("Habit",service.getById(12L,6L).getName());
        HabitRequest changed=request(HabitFrequency.WEEKLY,4,LocalDate.now().minusDays(4),null);
        when(habits.save(habit)).thenReturn(habit);
        assertEquals(4,service.update(12L,6L,changed).getTargetDaysPerWeek());
        when(logs.deleteAllByHabit_Id(6L)).thenReturn(0);
        service.delete(12L,6L);verify(habits).delete(habit);
    }

    @Test void rejectsCrossUserHabitAndInvalidDatesOrFrequency() {
        when(habits.findByIdAndUser_Id(6L,12L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->service.getById(12L,6L));
        HabitRequest invalid=request(HabitFrequency.DAILY,0,LocalDate.now(),null);
        assertThrows(BadRequestException.class,()->service.create(12L,invalid));
        assertThrows(BadRequestException.class,()->service.create(12L,request(HabitFrequency.WEEKLY,2,LocalDate.now(),LocalDate.now().minusDays(1))));
    }

    @Test void rejectsDuplicateDailyLogAndPreventsFutureLogs() {
        Habit habit=habit(HabitFrequency.DAILY,7,LocalDate.now().minusDays(10),null);
        when(habits.findByIdAndUser_Id(6L,12L)).thenReturn(Optional.of(habit));
        when(logs.existsByHabit_IdAndCompletionDate(6L,LocalDate.now())).thenReturn(true);
        assertThrows(DuplicateResourceException.class,()->service.createLog(12L,6L,new HabitLogRequest(LocalDate.now(),true,null)));
        assertThrows(BadRequestException.class,()->service.createLog(12L,6L,new HabitLogRequest(LocalDate.now().plusDays(1),true,null)));
    }

    @Test void completesExistingMissedLogIdempotentlyAndRejectsDeactivatedHabit() {
        Habit habit=habit(HabitFrequency.DAILY,7,LocalDate.now().minusDays(2),null);
        HabitLog log=log(habit,LocalDate.now(),false);
        when(habits.findByIdAndUser_Id(6L,12L)).thenReturn(Optional.of(habit));
        when(logs.findByHabit_IdAndCompletionDate(6L,LocalDate.now())).thenReturn(Optional.of(log));
        when(logs.save(log)).thenReturn(log);
        assertTrue(service.complete(12L,6L,new HabitCompletionRequest(LocalDate.now(),"done")).getCompleted());
        assertTrue(log.getCompleted());
        verify(notificationEvents).habitCompleted(log);
        habit.setActive(false);
        assertThrows(BadRequestException.class,()->service.complete(12L,6L,new HabitCompletionRequest(LocalDate.now(),null)));
    }

    @Test void undoAndRedoUpdateTheExistingCompletionLog() {
        Habit habit=habit(HabitFrequency.DAILY,7,LocalDate.now().minusDays(5),null);
        HabitLog log=log(habit,LocalDate.now().minusDays(1),true);log.setId(41L);
        when(habits.findByIdAndUser_Id(6L,12L)).thenReturn(Optional.of(habit));
        when(logs.findByHabit_IdAndCompletionDate(6L,log.getCompletionDate())).thenReturn(Optional.of(log));
        when(logs.save(log)).thenReturn(log);

        assertFalse(service.complete(12L,6L,new HabitCompletionRequest(log.getCompletionDate(),null,false)).getCompleted());
        assertFalse(log.getCompleted());
        assertTrue(service.complete(12L,6L,new HabitCompletionRequest(log.getCompletionDate(),null,true)).getCompleted());
        assertEquals(41L,log.getId());
        verify(logs,times(2)).save(log);
        verify(notificationEvents).habitCompleted(log);
    }

    @Test void weeklyAndMonthlyTargetsLimitEligibleCompletionDates() {
        LocalDate today=LocalDate.now(),weekStart=today.with(java.time.DayOfWeek.MONDAY);
        Habit weekly=habit(HabitFrequency.WEEKLY,2,today.minusDays(30),null);
        when(habits.findByIdAndUser_Id(6L,12L)).thenReturn(Optional.of(weekly));
        when(logs.findByHabit_IdAndCompletionDate(6L,today)).thenReturn(Optional.empty());
        when(logs.countByHabit_IdAndCompletedTrueAndCompletionDateBetween(6L,weekStart,weekStart.plusDays(6))).thenReturn(2L);
        assertThrows(BadRequestException.class,()->service.complete(12L,6L,new HabitCompletionRequest(today,null,true)));

        Habit monthly=habit(HabitFrequency.MONTHLY,1,today.minusDays(60),null);
        when(habits.findByIdAndUser_Id(6L,12L)).thenReturn(Optional.of(monthly));
        when(logs.countByHabit_IdAndCompletedTrueAndCompletionDateBetween(6L,YearMonth.now().atDay(1),YearMonth.now().atEndOfMonth())).thenReturn(1L);
        assertThrows(BadRequestException.class,()->service.complete(12L,6L,new HabitCompletionRequest(today,null,true)));
    }

    @Test void monthlyStreakAndConsistencyUseCalendarMonthSlots() {
        LocalDate today=LocalDate.now();YearMonth month=YearMonth.now();
        Habit monthly=habit(HabitFrequency.MONTHLY,1,month.minusMonths(1).atDay(1),null);
        when(habits.findByIdAndUser_Id(6L,12L)).thenReturn(Optional.of(monthly));
        when(logs.findAllByHabit_IdAndCompletedTrueAndCompletionDateLessThanEqualOrderByCompletionDateAsc(6L,today))
                .thenReturn(List.of(log(monthly,month.minusMonths(1).atDay(5),true),log(monthly,today,true)));

        HabitStreakResponse streak=service.streak(12L,6L);

        assertEquals(2,streak.getCurrentStreak());
        assertEquals(2,streak.getLongestStreak());
        assertEquals("months",streak.getPeriodUnit());
        assertEquals(100.0,streak.getCompletionPercentage());
    }

    @Test void calculatesDailyCurrentLongestAndMissedDayStreak() {
        LocalDate today=LocalDate.now();Habit daily=habit(HabitFrequency.DAILY,7,today.minusDays(5),null);
        when(habits.findByIdAndUser_Id(6L,12L)).thenReturn(Optional.of(daily));
        when(logs.findAllByHabit_IdAndCompletedTrueAndCompletionDateLessThanEqualOrderByCompletionDateAsc(6L,today))
                .thenReturn(List.of(log(daily,today.minusDays(4),true),log(daily,today.minusDays(2),true),log(daily,today.minusDays(1),true),log(daily,today,true)));
        HabitStreakResponse streak=service.streak(12L,6L);
        assertEquals(3,streak.getCurrentStreak());assertEquals(3,streak.getLongestStreak());assertEquals(4,streak.getTotalCompletedDays());
        when(logs.findAllByHabit_IdAndCompletedTrueAndCompletionDateLessThanEqualOrderByCompletionDateAsc(6L,today))
                .thenReturn(List.of(log(daily,today.minusDays(3),true),log(daily,today.minusDays(1),true)));
        assertEquals(1,service.streak(12L,6L).getCurrentStreak());
    }

    @Test void calculatesWeeklyStreakByTargetWeeksNotWeekendOnlyRuns() {
        LocalDate today=LocalDate.now(),thisMonday=today.with(java.time.DayOfWeek.MONDAY);
        Habit weekly=habit(HabitFrequency.WEEKLY,3,thisMonday.minusWeeks(4),null);
        when(habits.findByIdAndUser_Id(6L,12L)).thenReturn(Optional.of(weekly));
        List<HabitLog> entries=new java.util.ArrayList<>();
        for(int w=1;w<=3;w++){LocalDate monday=thisMonday.minusWeeks(w);entries.add(log(weekly,monday,true));entries.add(log(weekly,monday.plusDays(2),true));entries.add(log(weekly,monday.plusDays(5),true));}
        entries.add(log(weekly,thisMonday.plusDays(5),true));entries.add(log(weekly,thisMonday.plusDays(6),true));
        when(logs.findAllByHabit_IdAndCompletedTrueAndCompletionDateLessThanEqualOrderByCompletionDateAsc(6L,today)).thenReturn(entries);
        HabitStreakResponse streak=service.streak(12L,6L);
        assertEquals(3,streak.getCurrentStreak());assertEquals(3,streak.getLongestStreak());assertEquals("weeks",streak.getPeriodUnit());
    }

    @Test void generatesCalendarAndUserStatistics() {
        LocalDate today=LocalDate.now();YearMonth month=YearMonth.now();Habit habit=habit(HabitFrequency.DAILY,7,month.atDay(1),null);
        when(habits.findByIdAndUser_Id(6L,12L)).thenReturn(Optional.of(habit));
        when(logs.findAllByHabit_IdAndCompletionDateBetweenOrderByCompletionDateAsc(6L,month.atDay(1).with(java.time.DayOfWeek.MONDAY),today))
                .thenReturn(List.of(log(habit,today,true)));
        HabitCalendarResponse calendar=service.calendar(12L,6L,month.getYear(),month.getMonthValue());
        assertEquals(month.lengthOfMonth(),calendar.getDays().size());assertEquals(1,calendar.getCompletedDays());
        assertEquals(today.getDayOfMonth(),calendar.getEligibleDays());
        assertTrue(calendar.getDays().get(today.getDayOfMonth()-1).isEligible());
        assertFalse(calendar.getDays().get(today.getDayOfMonth()).isEligible());

        when(habits.findAllByUser_IdOrderByNameAsc(12L)).thenReturn(List.of(habit));
        when(logs.findAllByHabit_User_Id(12L)).thenReturn(List.of(log(habit,today,true)));
        when(habits.countByUser_IdAndActiveTrue(12L)).thenReturn(1L);
        HabitStatisticsResponse stats=service.statistics(12L);
        assertEquals(1L,stats.getTotalHabits());assertEquals(1L,stats.getTotalCompletedDays());assertTrue(stats.getMonthlyConsistencyPercentage()>0);
    }

    @Test void deactivationPreservesLogsAndHistoryRemainsAccessible() {
        Habit habit=habit(HabitFrequency.DAILY,7,LocalDate.now().minusDays(2),null);
        when(habits.findByIdAndUser_Id(6L,12L)).thenReturn(Optional.of(habit));when(habits.save(habit)).thenReturn(habit);
        service.deactivate(12L,6L);assertFalse(habit.getActive());verify(logs,never()).deleteAllByHabit_Id(6L);
        when(logs.findAllByHabit_IdOrderByCompletionDateDesc(6L)).thenReturn(List.of(log(habit,LocalDate.now().minusDays(1),true)));
        assertEquals(1,service.getLogs(12L,6L).size());
    }

    private HabitRequest request(HabitFrequency f,int target,LocalDate start,LocalDate end){return new HabitRequest("Habit","Description",f,target,start,end);}
    private Habit habit(HabitFrequency f,int target,LocalDate start,LocalDate end){Habit h=new Habit();h.setId(6L);h.setUser(owner);h.setName("Habit");h.setFrequency(f);h.setTargetDaysPerWeek(target);h.setStartDate(start);h.setEndDate(end);h.setActive(true);return h;}
    private HabitLog log(Habit h,LocalDate date,boolean completed){HabitLog log=new HabitLog();log.setHabit(h);log.setCompletionDate(date);log.setCompleted(completed);return log;}
}

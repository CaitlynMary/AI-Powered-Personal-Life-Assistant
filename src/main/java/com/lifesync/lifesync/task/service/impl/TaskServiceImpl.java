package com.lifesync.lifesync.task.service.impl;

import com.lifesync.lifesync.category.entity.Category;
import com.lifesync.lifesync.category.repository.CategoryRepository;
import com.lifesync.lifesync.calendar.repository.CalendarEventRepository;
import com.lifesync.lifesync.common.ApiConstants;
import com.lifesync.lifesync.goal.entity.Goal;
import com.lifesync.lifesync.goal.repository.GoalRepository;
import com.lifesync.lifesync.milestone.entity.GoalMilestone;
import com.lifesync.lifesync.milestone.repository.GoalMilestoneRepository;
import com.lifesync.lifesync.milestone.service.MilestoneService;
import com.lifesync.lifesync.notification.service.NotificationEventService;
import com.lifesync.lifesync.notification.repository.NotificationRepository;
import com.lifesync.lifesync.exception.BadRequestException;
import com.lifesync.lifesync.exception.DuplicateResourceException;
import com.lifesync.lifesync.exception.ResourceNotFoundException;
import com.lifesync.lifesync.reminder.entity.Reminder;
import com.lifesync.lifesync.reminder.entity.ReminderStatus;
import com.lifesync.lifesync.reminder.repository.ReminderRepository;
import com.lifesync.lifesync.schedule.repository.ScheduleSlotRepository;
import com.lifesync.lifesync.task.dto.*;
import com.lifesync.lifesync.task.entity.*;
import com.lifesync.lifesync.task.repository.TaskRepository;
import com.lifesync.lifesync.task.service.TaskLifecycleService;
import com.lifesync.lifesync.task.service.TaskService;
import com.lifesync.lifesync.user.entity.User;
import com.lifesync.lifesync.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class TaskServiceImpl implements TaskService {
    private static final Set<String> SORT_FIELDS=Set.of("createdAt","updatedAt","title","dueDate","priority","status","startDate");
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final GoalRepository goalRepository;
    private final GoalMilestoneRepository milestoneRepository;
    private final ReminderRepository reminderRepository;
    private final ScheduleSlotRepository scheduleRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationEventService notificationEvents;
    private final TaskLifecycleService lifecycle;
    private final MilestoneService milestoneService;
    public TaskServiceImpl(TaskRepository taskRepository,UserRepository userRepository,CategoryRepository categoryRepository,
                           GoalRepository goalRepository,GoalMilestoneRepository milestoneRepository) {
        this(taskRepository,userRepository,categoryRepository,goalRepository,milestoneRepository,null,null,null,null,new TaskLifecycleService(),null);
    }
    public TaskServiceImpl(TaskRepository taskRepository,UserRepository userRepository,CategoryRepository categoryRepository,
                           GoalRepository goalRepository,GoalMilestoneRepository milestoneRepository,ReminderRepository reminderRepository) {
        this(taskRepository,userRepository,categoryRepository,goalRepository,milestoneRepository,reminderRepository,null,null,null,new TaskLifecycleService(),null);
    }
    public TaskServiceImpl(TaskRepository taskRepository,UserRepository userRepository,CategoryRepository categoryRepository,
                           GoalRepository goalRepository,GoalMilestoneRepository milestoneRepository,ReminderRepository reminderRepository,
                           NotificationEventService notificationEvents,TaskLifecycleService lifecycle) {
        this(taskRepository,userRepository,categoryRepository,goalRepository,milestoneRepository,reminderRepository,
                null,null,notificationEvents,lifecycle,null);
    }
    public TaskServiceImpl(TaskRepository taskRepository,UserRepository userRepository,CategoryRepository categoryRepository,
                           GoalRepository goalRepository,GoalMilestoneRepository milestoneRepository,ReminderRepository reminderRepository,
                           NotificationEventService notificationEvents,TaskLifecycleService lifecycle,MilestoneService milestoneService) {
        this(taskRepository,userRepository,categoryRepository,goalRepository,milestoneRepository,reminderRepository,
                null,null,null,notificationEvents,lifecycle,milestoneService);
    }
    public TaskServiceImpl(TaskRepository taskRepository,UserRepository userRepository,CategoryRepository categoryRepository,
                           GoalRepository goalRepository,GoalMilestoneRepository milestoneRepository,ReminderRepository reminderRepository,
                           ScheduleSlotRepository scheduleRepository,CalendarEventRepository calendarEventRepository,
                           NotificationEventService notificationEvents,TaskLifecycleService lifecycle,MilestoneService milestoneService) {
        this(taskRepository,userRepository,categoryRepository,goalRepository,milestoneRepository,reminderRepository,
                scheduleRepository,calendarEventRepository,null,notificationEvents,lifecycle,milestoneService);
    }
    @Autowired
    public TaskServiceImpl(TaskRepository taskRepository,UserRepository userRepository,CategoryRepository categoryRepository,
                           GoalRepository goalRepository,GoalMilestoneRepository milestoneRepository,ReminderRepository reminderRepository,
                           ScheduleSlotRepository scheduleRepository,CalendarEventRepository calendarEventRepository,
                           NotificationRepository notificationRepository,NotificationEventService notificationEvents,
                           TaskLifecycleService lifecycle,MilestoneService milestoneService) {
        this.taskRepository=taskRepository;this.userRepository=userRepository;this.categoryRepository=categoryRepository;
        this.goalRepository=goalRepository;this.milestoneRepository=milestoneRepository;this.reminderRepository=reminderRepository;
        this.scheduleRepository=scheduleRepository;this.calendarEventRepository=calendarEventRepository;
        this.notificationRepository=notificationRepository;
        this.notificationEvents=notificationEvents;this.lifecycle=lifecycle;this.milestoneService=milestoneService;
    }
    @Override @Transactional
    public TaskResponse create(Long userId,TaskRequest request) {
        validateRecurrence(request.getRecurring(),request.getRecurrenceType()); validateDates(request.getStartDate(),request.getDueDate());
        Task task=new Task(); task.setUser(findUser(userId)); apply(task,request,userId);
        task.setStatus(TaskStatus.TODO); task.setActualDuration(null); task.setCompletedAt(null);
        Task saved=taskRepository.save(task);
        synchronizeTaskMilestone(userId,saved.getMilestone()==null?null:saved.getMilestone().getId());
        return response(saved);
    }
    @Override @Transactional(readOnly=true)
    public TaskPageResponse getAll(Long userId,int page,int size,String sortBy,String direction,TaskLifecycleStatus status,TaskPriority priority,Long categoryId,LocalDateTime dueFrom,LocalDateTime dueTo,String search) {
        if(page<0) throw new BadRequestException("Page must be zero or greater");
        if(size<1||size>ApiConstants.MAX_PAGE_SIZE) throw new BadRequestException("Page size must be between 1 and " + ApiConstants.MAX_PAGE_SIZE);
        if(!SORT_FIELDS.contains(sortBy)) throw new BadRequestException("Unsupported sort field");
        Sort.Direction sortDirection;
        try { sortDirection=Sort.Direction.fromString(direction); } catch(IllegalArgumentException ex) { throw new BadRequestException("Direction must be ASC or DESC"); }
        if(dueFrom!=null&&dueTo!=null&&dueFrom.isAfter(dueTo)) throw new BadRequestException("dueFrom must be before or equal to dueTo");
        Pageable pageable=PageRequest.of(page,size,Sort.by(sortDirection,sortBy));
        Specification<Task> spec=(root,query,cb)->cb.equal(root.get("user").get("id"),userId);
        if(status!=null) {
            LocalDateTime now=LocalDateTime.now();
            if(status==TaskLifecycleStatus.OVERDUE) {
                spec=spec.and((root,query,cb)->cb.and(
                        root.get("status").in(TaskStatus.TODO,TaskStatus.IN_PROGRESS),
                        cb.isNotNull(root.get("dueDate")),
                        cb.lessThan(root.get("dueDate"),now)));
            } else if(status==TaskLifecycleStatus.TODO||status==TaskLifecycleStatus.IN_PROGRESS) {
                TaskStatus storedStatus=TaskStatus.valueOf(status.name());
                spec=spec.and((root,query,cb)->cb.and(
                        cb.equal(root.get("status"),storedStatus),
                        cb.or(cb.isNull(root.get("dueDate")),cb.greaterThanOrEqualTo(root.get("dueDate"),now))));
            } else {
                TaskStatus storedStatus=TaskStatus.valueOf(status.name());
                spec=spec.and((root,query,cb)->cb.equal(root.get("status"),storedStatus));
            }
        }
        if(priority!=null) spec=spec.and((root,query,cb)->cb.equal(root.get("priority"),priority));
        if(categoryId!=null) spec=spec.and((root,query,cb)->cb.equal(root.get("category").get("id"),categoryId));
        if(dueFrom!=null) spec=spec.and((root,query,cb)->cb.greaterThanOrEqualTo(root.get("dueDate"),dueFrom));
        if(dueTo!=null) spec=spec.and((root,query,cb)->cb.lessThanOrEqualTo(root.get("dueDate"),dueTo));
        if(search!=null&&!search.isBlank()) {
            String pattern="%"+search.trim().toLowerCase(java.util.Locale.ROOT)+"%";
            spec=spec.and((root,query,cb)->cb.or(
                    cb.like(cb.lower(root.get("title")),pattern),
                    cb.like(cb.lower(root.get("description")),pattern)));
        }
        Page<Task> result=taskRepository.findAll(spec,pageable);
        return new TaskPageResponse(result.getContent().stream().map(this::response).toList(),result.getNumber(),result.getSize(),result.getTotalElements(),result.getTotalPages(),result.isLast());
    }
    @Override @Transactional(readOnly=true)
    public TaskResponse getById(Long userId,Long id) { return response(findTask(userId,id)); }
    @Override @Transactional
    public TaskResponse update(Long userId,Long id,UpdateTaskRequest request) {
        validateRecurrence(request.getRecurring(),request.getRecurrenceType()); validateDates(request.getStartDate(),request.getDueDate());
        Task task=findTask(userId,id);
        Long previousMilestoneId=task.getMilestone()==null?null:task.getMilestone().getId();
        LocalDateTime previousDueDate=task.getDueDate();
        apply(task,request,userId);
        if(!Objects.equals(previousDueDate,task.getDueDate())) {
            synchronizeTaskReminders(task);
            if(notificationEvents!=null&&(task.getStatus()==TaskStatus.TODO||task.getStatus()==TaskStatus.IN_PROGRESS))
                notificationEvents.taskRescheduled(task,previousDueDate);
        }
        Task saved=taskRepository.save(task);
        Long nextMilestoneId=saved.getMilestone()==null?null:saved.getMilestone().getId();
        if(!Objects.equals(previousMilestoneId,nextMilestoneId)){
            synchronizeTaskMilestone(userId,previousMilestoneId);
            synchronizeTaskMilestone(userId,nextMilestoneId);
        }
        return response(saved);
    }
    @Override @Transactional
    public TaskResponse updateStatus(Long userId,Long id,TaskStatus status) {
        Task task=findTask(userId,id);
        TaskStatus previousStatus=task.getStatus();
        setStatus(task,status);
        Task saved=taskRepository.save(task);
        if(previousStatus!=status)synchronizeTaskMilestone(userId,saved.getMilestone()==null?null:saved.getMilestone().getId());
        return response(saved);
    }
    @Override @Transactional
    public TaskResponse complete(Long userId,Long id) {
        Task task=findTask(userId,id);
        TaskStatus previousStatus=task.getStatus();
        setStatus(task,TaskStatus.COMPLETED);
        Task saved=taskRepository.save(task);
        if(previousStatus!=TaskStatus.COMPLETED)
            synchronizeTaskMilestone(userId,saved.getMilestone()==null?null:saved.getMilestone().getId());
        return response(saved);
    }
    @Override @Transactional
    public void delete(Long userId,Long id) {
        Task task=findTask(userId,id);
        if(scheduleRepository!=null&&scheduleRepository.existsByTask_Id(id))
            throw new DuplicateResourceException(
                    "This task is linked to a planner schedule. Remove or unlink the schedule entry before deleting the task.");
        if(calendarEventRepository!=null&&calendarEventRepository.existsByTask_Id(id))
            throw new DuplicateResourceException(
                    "This task is linked to a calendar event. Remove or unlink the event before deleting the task.");
        if(reminderRepository!=null)reminderRepository.clearTaskReferences(id,LocalDateTime.now());
        if(notificationRepository!=null)notificationRepository.clearTaskReferences(id);
        Long milestoneId=task.getMilestone()==null?null:task.getMilestone().getId();
        taskRepository.delete(task);
        synchronizeTaskMilestone(userId,milestoneId);
    }
    @Override @Transactional(readOnly=true)
    public TaskStatisticsResponse statistics(Long userId) {
        LocalDate today=LocalDate.now();
        LocalDateTime now=LocalDateTime.now();
        long pending=taskRepository.countNotOverdueByUserAndStatus(userId,TaskStatus.TODO,now);
        long inProgress=taskRepository.countNotOverdueByUserAndStatus(userId,TaskStatus.IN_PROGRESS,now);
        long active=pending+inProgress;
        return new TaskStatisticsResponse(taskRepository.countByUser_Id(userId),active,
                pending,
                inProgress,
                taskRepository.countByUser_IdAndStatus(userId,TaskStatus.COMPLETED),
                taskRepository.countByUser_IdAndStatus(userId,TaskStatus.MISSED),
                taskRepository.countByUser_IdAndStatus(userId,TaskStatus.CANCELLED),
                taskRepository.countOverdue(userId,now,List.of(TaskStatus.COMPLETED,TaskStatus.CANCELLED,TaskStatus.MISSED)),
                taskRepository.countDueBetween(userId,today.atStartOfDay(),today.plusDays(1).atStartOfDay()),
                taskRepository.countByUser_IdAndPriorityIn(userId,List.of(TaskPriority.HIGH,TaskPriority.URGENT)));
    }
    private void apply(Task task,TaskRequest request,Long userId) {
        task.setTitle(request.getTitle().trim()); task.setDescription(blankToNull(request.getDescription()));
        task.setCategory(resolveCategory(userId,request.getCategoryId())); task.setPriority(request.getPriority());
        applyGoalReferences(task,userId,request.getGoalId(),request.getMilestoneId());
        task.setDueDate(request.getDueDate()); task.setStartDate(request.getStartDate()); task.setEstimatedDuration(request.getEstimatedDuration());
        task.setRecurring(request.getRecurring()); task.setRecurrenceType(request.getRecurrenceType());
    }
    private void apply(Task task,UpdateTaskRequest request,Long userId) {
        task.setTitle(request.getTitle().trim()); task.setDescription(blankToNull(request.getDescription()));
        task.setCategory(resolveCategory(userId,request.getCategoryId())); task.setPriority(request.getPriority());
        applyGoalReferences(task,userId,request.getGoalId(),request.getMilestoneId());
        task.setDueDate(request.getDueDate()); task.setStartDate(request.getStartDate()); task.setEstimatedDuration(request.getEstimatedDuration());
        task.setRecurring(request.getRecurring()); task.setRecurrenceType(request.getRecurrenceType());
    }
    private Category resolveCategory(Long userId,Long categoryId) {
        if(categoryId==null) return null;
        return categoryRepository.findByIdAndUser_Id(categoryId,userId).orElseThrow(()->new ResourceNotFoundException("Category was not found"));
    }
    private void applyGoalReferences(Task task,Long userId,Long goalId,Long milestoneId) {
        Goal goal=null;
        GoalMilestone milestone=null;
        if(goalId!=null) {
            if(goalRepository==null) throw new IllegalStateException("Goal management is not configured");
            goal=goalRepository.findByIdAndUser_Id(goalId,userId).orElseThrow(()->new ResourceNotFoundException("Goal was not found"));
        }
        if(milestoneId!=null) {
            if(milestoneRepository==null) throw new IllegalStateException("Milestone management is not configured");
            milestone=milestoneRepository.findByIdAndGoal_User_Id(milestoneId,userId).orElseThrow(()->new ResourceNotFoundException("Milestone was not found"));
            if(goal!=null&&!milestone.getGoal().getId().equals(goal.getId())) throw new BadRequestException("Milestone does not belong to the selected goal");
            if(goal==null) goal=milestone.getGoal();
        }
        task.setGoal(goal);
        task.setMilestone(milestone);
    }
    private User findUser(Long id) { return userRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("User account was not found")); }
    private Task findTask(Long userId,Long id) { return taskRepository.findByIdAndUser_Id(id,userId).orElseThrow(()->new ResourceNotFoundException("Task was not found")); }
    private void validateRecurrence(Boolean recurring,RecurrenceType type) {
        if(recurring==null||type==null) throw new BadRequestException("Recurrence settings are required");
        if(!recurring&&type!=RecurrenceType.NONE) throw new BadRequestException("Non-recurring tasks must use recurrenceType NONE");
        if(recurring&&type==RecurrenceType.NONE) throw new BadRequestException("Recurring tasks must use DAILY, WEEKLY, MONTHLY, or YEARLY recurrence");
    }
    private void validateDates(LocalDateTime start,LocalDateTime due) { if(start!=null&&due!=null&&start.isAfter(due)) throw new BadRequestException("startDate cannot be after dueDate"); }
    private void setStatus(Task task,TaskStatus status) {
        if(status==null) throw new BadRequestException("Task status is required");
        TaskStatus previous=task.getStatus();
        if(previous==TaskStatus.COMPLETED&&status==TaskStatus.TODO
                &&(task.getStatusBeforeCompletion()==TaskStatus.TODO||task.getStatusBeforeCompletion()==TaskStatus.IN_PROGRESS)) {
            status=task.getStatusBeforeCompletion();
        }
        if(previous!=status&&!isAllowedTransition(previous,status)) {
            throw new BadRequestException("Cannot change task status from "+previous+" to "+status);
        }
        LocalDateTime now=LocalDateTime.now();
        if(status==TaskStatus.COMPLETED) {
            if(previous==TaskStatus.TODO||previous==TaskStatus.IN_PROGRESS) task.setStatusBeforeCompletion(previous);
            if(previous!=TaskStatus.COMPLETED||task.getCompletedAt()==null) task.setCompletedAt(now);
            task.setMissedAt(null);
        } else if(status==TaskStatus.MISSED) {
            if(previous!=TaskStatus.MISSED||task.getMissedAt()==null) task.setMissedAt(now);
            task.setCompletedAt(null);
            task.setStatusBeforeCompletion(null);
        } else {
            task.setCompletedAt(null);
            task.setMissedAt(null);
            task.setStatusBeforeCompletion(null);
        }
        task.setStatus(status);
        if(previous!=status&&notificationEvents!=null) {
            if(status==TaskStatus.COMPLETED) notificationEvents.taskCompleted(task);
            if(status==TaskStatus.MISSED) notificationEvents.taskMissed(task);
        }
        if(status==TaskStatus.COMPLETED||status==TaskStatus.CANCELLED||status==TaskStatus.MISSED) {
            cancelPendingTaskReminders(task);
        }
    }
    private boolean isAllowedTransition(TaskStatus current,TaskStatus next) {
        return switch(current) {
            case TODO,IN_PROGRESS -> next==TaskStatus.IN_PROGRESS||next==TaskStatus.COMPLETED||next==TaskStatus.CANCELLED||next==TaskStatus.MISSED||next==TaskStatus.TODO;
            case COMPLETED -> next==TaskStatus.TODO||next==TaskStatus.IN_PROGRESS;
            case MISSED -> next==TaskStatus.TODO||next==TaskStatus.IN_PROGRESS;
            case CANCELLED -> next==TaskStatus.TODO;
        };
    }
    private void cancelPendingTaskReminders(Task task) {
        if(reminderRepository==null||task.getId()==null) return;
        List<Reminder> pending=reminderRepository.findAllByUser_IdAndTask_IdAndStatus(
                task.getUser().getId(),task.getId(),ReminderStatus.PENDING);
        for(Reminder reminder:pending) {
            reminder.setStatus(ReminderStatus.CANCELLED);
            reminderRepository.save(reminder);
        }
    }
    private void synchronizeTaskReminders(Task task) {
        if(reminderRepository==null||task.getId()==null) return;
        List<Reminder> pending=reminderRepository.findAllByUser_IdAndTask_IdAndStatus(
                task.getUser().getId(),task.getId(),ReminderStatus.PENDING);
        for(Reminder reminder:pending) {
            if(task.getStatus()==TaskStatus.COMPLETED||task.getStatus()==TaskStatus.CANCELLED
                    ||task.getStatus()==TaskStatus.MISSED||task.getDueDate()==null) {
                reminder.setStatus(ReminderStatus.CANCELLED);
            } else if(reminder.getDeadlineOffsetMinutes()!=null) {
                reminder.setReminderDateTime(task.getDueDate().minusMinutes(reminder.getDeadlineOffsetMinutes()));
            } else if(reminder.getReminderDateTime().isAfter(task.getDueDate())) {
                reminder.setStatus(ReminderStatus.CANCELLED);
            }
            reminderRepository.save(reminder);
        }
    }
    private void synchronizeTaskMilestone(Long userId,Long milestoneId) {
        if(milestoneService!=null&&milestoneId!=null)milestoneService.synchronizeTaskProgress(userId,milestoneId);
    }
    private String blankToNull(String value) { return value==null||value.isBlank()?null:value.trim(); }
    private TaskResponse response(Task t) {
        Category category=t.getCategory();
        Goal goal=t.getGoal(); GoalMilestone milestone=t.getMilestone();
        return new TaskResponse(t.getId(),t.getTitle(),t.getDescription(),category==null?null:category.getId(),category==null?null:category.getName(),goal==null?null:goal.getId(),milestone==null?null:milestone.getId(),t.getPriority(),lifecycle.statusOf(t,LocalDateTime.now()),t.getDueDate(),t.getStartDate(),t.getEstimatedDuration(),t.getActualDuration(),t.getCompletedAt(),t.getMissedAt(),t.getRecurring(),t.getRecurrenceType(),t.getCreatedAt(),t.getUpdatedAt());
    }
}

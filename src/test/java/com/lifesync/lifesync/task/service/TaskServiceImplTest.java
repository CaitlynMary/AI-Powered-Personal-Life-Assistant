package com.lifesync.lifesync.task.service;

import com.lifesync.lifesync.category.entity.Category;
import com.lifesync.lifesync.category.repository.CategoryRepository;
import com.lifesync.lifesync.calendar.repository.CalendarEventRepository;
import com.lifesync.lifesync.exception.BadRequestException;
import com.lifesync.lifesync.exception.ResourceNotFoundException;
import com.lifesync.lifesync.notification.service.NotificationEventService;
import com.lifesync.lifesync.milestone.service.MilestoneService;
import com.lifesync.lifesync.milestone.entity.GoalMilestone;
import com.lifesync.lifesync.reminder.entity.Reminder;
import com.lifesync.lifesync.reminder.entity.ReminderStatus;
import com.lifesync.lifesync.reminder.repository.ReminderRepository;
import com.lifesync.lifesync.schedule.repository.ScheduleSlotRepository;
import com.lifesync.lifesync.task.dto.*;
import com.lifesync.lifesync.task.entity.*;
import com.lifesync.lifesync.task.repository.TaskRepository;
import com.lifesync.lifesync.task.service.TaskLifecycleService;
import com.lifesync.lifesync.task.service.impl.TaskServiceImpl;
import com.lifesync.lifesync.user.entity.User;
import com.lifesync.lifesync.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {
    @Mock TaskRepository tasks;
    @Mock UserRepository users;
    @Mock CategoryRepository categories;
    @Mock com.lifesync.lifesync.goal.repository.GoalRepository goals;
    @Mock com.lifesync.lifesync.milestone.repository.GoalMilestoneRepository milestones;
    @Mock ReminderRepository reminders;
    @Mock ScheduleSlotRepository schedules;
    @Mock CalendarEventRepository calendarEvents;
    @Mock NotificationEventService notificationEvents;
    @Mock MilestoneService linkedMilestones;
    private TaskServiceImpl service;
    private User user;

    @BeforeEach void setUp() { service=new TaskServiceImpl(tasks,users,categories,goals,milestones); user=new User(); user.setId(7L); user.setEmail("owner@example.com"); }

    @Test void createsTaskForAuthenticatedOwnerWithInitialTodo() {
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(tasks.save(any(Task.class))).thenAnswer(i->{Task t=i.getArgument(0);t.setId(12L);return t;});
        TaskResponse response=service.create(7L,request(null,false,RecurrenceType.NONE));
        ArgumentCaptor<Task> captor=ArgumentCaptor.forClass(Task.class); verify(tasks).save(captor.capture());
        assertEquals(7L,captor.getValue().getUser().getId()); assertEquals(TaskStatus.TODO,captor.getValue().getStatus());
        assertEquals(12L,response.getId());
    }

    @Test void creatingTaskLinkedToMilestoneRecalculatesMilestoneProgress() {
        when(users.findById(7L)).thenReturn(Optional.of(user));
        com.lifesync.lifesync.goal.entity.Goal goal=new com.lifesync.lifesync.goal.entity.Goal();
        goal.setId(3L);
        goal.setUser(user);
        GoalMilestone milestone=new GoalMilestone();
        milestone.setId(6L);
        milestone.setGoal(goal);
        when(goals.findByIdAndUser_Id(3L,7L)).thenReturn(Optional.of(goal));
        when(milestones.findByIdAndGoal_User_Id(6L,7L)).thenReturn(Optional.of(milestone));
        when(tasks.save(any(Task.class))).thenAnswer(i->{Task t=i.getArgument(0);t.setId(12L);return t;});
        TaskServiceImpl serviceWithMilestoneSync=new TaskServiceImpl(tasks,users,categories,goals,milestones,
                reminders,notificationEvents,new TaskLifecycleService(),linkedMilestones);

        serviceWithMilestoneSync.create(7L,new TaskRequest("Linked",null,null,3L,6L,
                TaskPriority.MEDIUM,null,null,30,false,RecurrenceType.NONE));

        verify(linkedMilestones).synchronizeTaskProgress(7L,6L);
    }

    @Test void rejectsCategoryOwnedByAnotherUserAndHidesOtherUsersTask() {
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(categories.findByIdAndUser_Id(3L,7L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->service.create(7L,request(3L,false,RecurrenceType.NONE)));
        when(tasks.findByIdAndUser_Id(99L,7L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->service.getById(7L,99L));
        verify(tasks,never()).delete((Task) any());
    }

    @Test void associatesOnlyGoalsAndMilestonesOwnedByAuthenticatedUser() {
        when(users.findById(7L)).thenReturn(Optional.of(user));
        com.lifesync.lifesync.goal.entity.Goal goal=new com.lifesync.lifesync.goal.entity.Goal();goal.setId(3L);goal.setUser(user);
        com.lifesync.lifesync.milestone.entity.GoalMilestone milestone=new com.lifesync.lifesync.milestone.entity.GoalMilestone();milestone.setId(6L);milestone.setGoal(goal);
        when(goals.findByIdAndUser_Id(3L,7L)).thenReturn(Optional.of(goal));
        when(milestones.findByIdAndGoal_User_Id(6L,7L)).thenReturn(Optional.of(milestone));
        when(tasks.save(any(Task.class))).thenAnswer(i->i.getArgument(0));
        TaskRequest linked=new TaskRequest("Linked",null,null,3L,6L,TaskPriority.MEDIUM,null,null,30,false,RecurrenceType.NONE);
        service.create(7L,linked);
        verify(tasks).save(argThat(t->t.getGoal().getId().equals(3L)&&t.getMilestone().getId().equals(6L)));
        com.lifesync.lifesync.goal.entity.Goal otherGoal=new com.lifesync.lifesync.goal.entity.Goal();otherGoal.setId(11L);otherGoal.setUser(user);
        when(goals.findByIdAndUser_Id(11L,7L)).thenReturn(Optional.of(otherGoal));
        assertThrows(BadRequestException.class,()->service.create(7L,new TaskRequest("Wrong",null,null,11L,6L,TaskPriority.LOW,null,null,10,false,RecurrenceType.NONE)));
    }

    @Test void updatesTaskAndDoesNotChangeOwnership() {
        Task task=task(); when(tasks.findByIdAndUser_Id(1L,7L)).thenReturn(Optional.of(task));
        when(tasks.save(task)).thenReturn(task);
        UpdateTaskRequest update=new UpdateTaskRequest("Updated","description",null,TaskPriority.URGENT,null,null,60,false,RecurrenceType.NONE);
        TaskResponse response=service.update(7L,1L,update);
        assertEquals(7L,task.getUser().getId()); assertEquals("Updated",response.getTitle()); assertEquals(TaskPriority.URGENT,response.getPriority());
    }

    @Test void completionSetsTimeAndMovingAwayClearsIt() {
        Task task=task(); when(tasks.findByIdAndUser_Id(1L,7L)).thenReturn(Optional.of(task)); when(tasks.save(task)).thenReturn(task);
        assertNotNull(service.complete(7L,1L).getCompletedAt());
        LocalDateTime completed=task.getCompletedAt();
        assertEquals(completed,service.complete(7L,1L).getCompletedAt());
        assertNull(service.updateStatus(7L,1L,TaskStatus.IN_PROGRESS).getCompletedAt());
    }

    @Test void taskCompletionAndReopenRecalculateItsLinkedMilestone() {
        Task task=task();
        GoalMilestone milestone=new GoalMilestone();
        milestone.setId(6L);
        task.setMilestone(milestone);
        when(tasks.findByIdAndUser_Id(1L,7L)).thenReturn(Optional.of(task),Optional.of(task));
        when(tasks.save(task)).thenReturn(task);
        TaskServiceImpl serviceWithMilestoneSync=new TaskServiceImpl(tasks,users,categories,goals,milestones,
                reminders,notificationEvents,new TaskLifecycleService(),linkedMilestones);

        serviceWithMilestoneSync.complete(7L,1L);
        serviceWithMilestoneSync.updateStatus(7L,1L,TaskStatus.TODO);

        verify(linkedMilestones,times(2)).synchronizeTaskProgress(7L,6L);
    }

    @Test void reopeningCompletedTaskRestoresItsPreviousStateAndRecalculatesOverdue() {
        Task task=task();
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setDueDate(LocalDateTime.now().minusMinutes(1));
        when(tasks.findByIdAndUser_Id(1L,7L)).thenReturn(Optional.of(task),Optional.of(task));
        when(tasks.save(task)).thenReturn(task);

        service.complete(7L,1L);
        assertEquals(TaskStatus.IN_PROGRESS,task.getStatusBeforeCompletion());

        TaskResponse reopened=service.updateStatus(7L,1L,TaskStatus.TODO);

        assertEquals(TaskLifecycleStatus.OVERDUE,reopened.getStatus());
        assertEquals(TaskStatus.IN_PROGRESS,task.getStatus());
        assertNull(task.getCompletedAt());
        assertNull(task.getStatusBeforeCompletion());
    }

    @Test void calculatesOverdueOnReadWithoutPersistingAnOverdueStatus() {
        Task task=task();
        task.setDueDate(LocalDateTime.now().minusMinutes(1));
        when(tasks.findByIdAndUser_Id(1L,7L)).thenReturn(Optional.of(task));

        TaskResponse response=service.getById(7L,1L);

        assertEquals(TaskLifecycleStatus.OVERDUE,response.getStatus());
        assertEquals(TaskStatus.TODO,task.getStatus());
    }

    @Test void missedTasksCanBeRestoredAndCompletionTimestampsStayConsistent() {
        Task task=task(); when(tasks.findByIdAndUser_Id(1L,7L)).thenReturn(Optional.of(task)); when(tasks.save(task)).thenReturn(task);
        assertEquals("MISSED",service.updateStatus(7L,1L,TaskStatus.MISSED).getStatus().name());
        assertNotNull(task.getMissedAt());
        service.updateStatus(7L,1L,TaskStatus.IN_PROGRESS);
        assertNull(task.getMissedAt());
        service.complete(7L,1L);
        assertNotNull(task.getCompletedAt());
        assertThrows(BadRequestException.class,()->service.updateStatus(7L,1L,TaskStatus.MISSED));
    }

    @Test void terminalTaskTransitionsCancelPendingTaskReminders() {
        Task completed=task();
        Task missed=task();
        Task cancelled=task();
        when(tasks.findByIdAndUser_Id(1L,7L))
                .thenReturn(Optional.of(completed),Optional.of(missed),Optional.of(cancelled));
        when(tasks.save(any(Task.class))).thenAnswer(i->i.getArgument(0));
        Reminder completedReminder=pendingReminder();
        Reminder missedReminder=pendingReminder();
        Reminder cancelledReminder=pendingReminder();
        when(reminders.findAllByUser_IdAndTask_IdAndStatus(7L,1L,ReminderStatus.PENDING))
                .thenReturn(List.of(completedReminder),List.of(missedReminder),List.of(cancelledReminder));
        TaskServiceImpl serviceWithReminderSync=new TaskServiceImpl(tasks,users,categories,goals,milestones,
                reminders,notificationEvents,new TaskLifecycleService());

        serviceWithReminderSync.updateStatus(7L,1L,TaskStatus.COMPLETED);
        serviceWithReminderSync.updateStatus(7L,1L,TaskStatus.MISSED);
        serviceWithReminderSync.updateStatus(7L,1L,TaskStatus.CANCELLED);

        assertEquals(TaskStatus.COMPLETED,completed.getStatus());
        assertNotNull(completed.getCompletedAt());
        assertEquals(ReminderStatus.CANCELLED,completedReminder.getStatus());
        assertEquals(TaskStatus.MISSED,missed.getStatus());
        assertNotNull(missed.getMissedAt());
        assertEquals(ReminderStatus.CANCELLED,missedReminder.getStatus());
        assertEquals(TaskStatus.CANCELLED,cancelled.getStatus());
        assertNull(cancelled.getCompletedAt());
        assertNull(cancelled.getMissedAt());
        assertEquals(ReminderStatus.CANCELLED,cancelledReminder.getStatus());
        verify(reminders,times(3)).save(any(Reminder.class));
    }

    @Test void reschedulingTaskRecalculatesDeadlineOffsetReminders() {
        Task task=task();
        task.setStatus(TaskStatus.IN_PROGRESS);
        LocalDateTime previousDeadline=LocalDateTime.now().minusDays(1);
        LocalDateTime nextDeadline=LocalDateTime.now().plusDays(1);
        task.setDueDate(previousDeadline);
        Reminder reminder=new Reminder();
        reminder.setStatus(ReminderStatus.PENDING);
        reminder.setDeadlineOffsetMinutes(30);
        reminder.setReminderDateTime(previousDeadline.minusMinutes(30));
        when(tasks.findByIdAndUser_Id(1L,7L)).thenReturn(Optional.of(task));
        when(tasks.save(task)).thenReturn(task);
        when(reminders.findAllByUser_IdAndTask_IdAndStatus(7L,1L,ReminderStatus.PENDING))
                .thenReturn(List.of(reminder));
        TaskServiceImpl serviceWithReminderSync=new TaskServiceImpl(tasks,users,categories,goals,milestones,
                reminders,notificationEvents,new TaskLifecycleService());

        var response=serviceWithReminderSync.update(7L,1L,new UpdateTaskRequest(
                "Rescheduled",null,null,TaskPriority.HIGH,nextDeadline,null,60,false,RecurrenceType.NONE));

        assertEquals(nextDeadline,response.getDueDate());
        assertEquals(TaskLifecycleStatus.IN_PROGRESS,response.getStatus());
        assertEquals(nextDeadline.minusMinutes(30),reminder.getReminderDateTime());
        verify(reminders).save(reminder);
        verify(notificationEvents).taskRescheduled(task,previousDeadline);
    }

    @Test void doesNotPublishRescheduleNotificationsForTerminalTasks() {
        Task completed=task();
        completed.setStatus(TaskStatus.COMPLETED);
        completed.setDueDate(LocalDateTime.now().plusDays(1));
        Task cancelled=task();
        cancelled.setStatus(TaskStatus.CANCELLED);
        cancelled.setDueDate(LocalDateTime.now().plusDays(1));
        when(tasks.findByIdAndUser_Id(1L,7L)).thenReturn(Optional.of(completed),Optional.of(cancelled));
        when(tasks.save(any(Task.class))).thenAnswer(i->i.getArgument(0));
        TaskServiceImpl serviceWithLifecycleDependencies=new TaskServiceImpl(tasks,users,categories,goals,milestones,
                reminders,notificationEvents,new TaskLifecycleService());
        UpdateTaskRequest update=new UpdateTaskRequest("Updated",null,null,TaskPriority.HIGH,
                LocalDateTime.now().plusDays(2),null,30,false,RecurrenceType.NONE);

        serviceWithLifecycleDependencies.update(7L,1L,update);
        serviceWithLifecycleDependencies.update(7L,1L,update);

        verify(notificationEvents,never()).taskRescheduled(any(Task.class),any());
    }

    @Test void rejectsInconsistentRecurrenceAndInvalidDateOrder() {
        assertThrows(BadRequestException.class,()->service.create(7L,request(null,false,RecurrenceType.DAILY)));
        TaskRequest invalid=new TaskRequest("Title",null,null,TaskPriority.MEDIUM,LocalDateTime.now(),LocalDateTime.now().plusDays(1),30,true,RecurrenceType.WEEKLY);
        assertThrows(BadRequestException.class,()->service.create(7L,invalid));
        verify(tasks,never()).save(any());
    }

    @Test void scopesPaginationFiltersAndSearchToOwner() {
        when(tasks.findAll(any(Specification.class),any(Pageable.class))).thenReturn(new PageImpl<>(List.of(task())));
        TaskPageResponse page=service.getAll(7L,0,10,"createdAt","DESC",TaskLifecycleStatus.TODO,TaskPriority.HIGH,2L,null,null,"learn");
        assertEquals(1,page.getTotalElements()); assertEquals(1,page.getContent().size());
        ArgumentCaptor<Pageable> pageable=ArgumentCaptor.forClass(Pageable.class);
        verify(tasks).findAll(any(Specification.class),pageable.capture());
        assertEquals("createdAt: DESC",pageable.getValue().getSort().toString());
    }

    @Test void returnsUserTaskStatistics() {
        when(tasks.countByUser_Id(7L)).thenReturn(9L);
        when(tasks.countNotOverdueByUserAndStatus(eq(7L),eq(TaskStatus.TODO),any())).thenReturn(1L);
        when(tasks.countNotOverdueByUserAndStatus(eq(7L),eq(TaskStatus.IN_PROGRESS),any())).thenReturn(1L);
        when(tasks.countByUser_IdAndStatus(7L,TaskStatus.COMPLETED)).thenReturn(3L);
        when(tasks.countByUser_IdAndStatus(7L,TaskStatus.MISSED)).thenReturn(1L);
        when(tasks.countByUser_IdAndStatus(7L,TaskStatus.CANCELLED)).thenReturn(2L);
        when(tasks.countOverdue(eq(7L),any(),anyCollection())).thenReturn(2L);
        when(tasks.countDueBetween(eq(7L),any(),any())).thenReturn(1L);
        when(tasks.countByUser_IdAndPriorityIn(eq(7L),anyCollection())).thenReturn(4L);
        TaskStatisticsResponse stats=service.statistics(7L);
        assertEquals(9L,stats.getTotalTasks()); assertEquals(2L,stats.getActiveTasks()); assertEquals(1L,stats.getPendingTasks()); assertEquals(1L,stats.getInProgressTasks()); assertEquals(1L,stats.getMissedTasks()); assertEquals(2L,stats.getCancelledTasks()); assertEquals(2L,stats.getOverdueTasks());
        assertEquals(4L,stats.getHighPriorityTasks());
    }

    @Test void deletesOnlyOwnedTask() {
        Task task=task(); when(tasks.findByIdAndUser_Id(1L,7L)).thenReturn(Optional.of(task));
        service.delete(7L,1L); verify(tasks).delete(task);
    }

    @Test void refusesToDeleteTaskWithLinkedScheduleOrCalendarEvent() {
        Task task=task();
        when(tasks.findByIdAndUser_Id(1L,7L)).thenReturn(Optional.of(task),Optional.of(task));
        when(schedules.existsByTask_Id(1L)).thenReturn(true);
        when(calendarEvents.existsByTask_Id(1L)).thenReturn(true);
        TaskServiceImpl serviceWithReferenceChecks=new TaskServiceImpl(tasks,users,categories,goals,milestones,
                reminders,schedules,calendarEvents,notificationEvents,new TaskLifecycleService(),linkedMilestones);

        com.lifesync.lifesync.exception.DuplicateResourceException scheduleConflict=assertThrows(
                com.lifesync.lifesync.exception.DuplicateResourceException.class,
                ()->serviceWithReferenceChecks.delete(7L,1L));
        assertTrue(scheduleConflict.getMessage().contains("planner schedule"));

        when(schedules.existsByTask_Id(1L)).thenReturn(false);
        com.lifesync.lifesync.exception.DuplicateResourceException calendarConflict=assertThrows(
                com.lifesync.lifesync.exception.DuplicateResourceException.class,
                ()->serviceWithReferenceChecks.delete(7L,1L));
        assertTrue(calendarConflict.getMessage().contains("calendar event"));
        verify(tasks,never()).delete(any(Task.class));
    }

    private TaskRequest request(Long category,boolean recurring,RecurrenceType type) {
        return new TaskRequest("Complete study", "JPA", category, TaskPriority.HIGH, null, null, 60, recurring, type);
    }
    private Reminder pendingReminder() {
        Reminder reminder=new Reminder();
        reminder.setStatus(ReminderStatus.PENDING);
        return reminder;
    }
    private Task task() { Task t=new Task(); t.setId(1L);t.setUser(user);t.setTitle("Original");t.setPriority(TaskPriority.MEDIUM);t.setStatus(TaskStatus.TODO);t.setRecurring(false);t.setRecurrenceType(RecurrenceType.NONE);return t; }
}

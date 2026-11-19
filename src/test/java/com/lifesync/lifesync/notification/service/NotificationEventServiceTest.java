package com.lifesync.lifesync.notification.service;

import com.lifesync.lifesync.goal.entity.Goal;
import com.lifesync.lifesync.habit.entity.Habit;
import com.lifesync.lifesync.habit.entity.HabitLog;
import com.lifesync.lifesync.milestone.entity.GoalMilestone;
import com.lifesync.lifesync.notification.entity.Notification;
import com.lifesync.lifesync.notification.entity.NotificationType;
import com.lifesync.lifesync.notification.repository.NotificationRepository;
import com.lifesync.lifesync.reminder.entity.Reminder;
import com.lifesync.lifesync.task.repository.TaskRepository;
import com.lifesync.lifesync.task.entity.Task;
import com.lifesync.lifesync.user.entity.User;
import com.lifesync.lifesync.user.repository.UserRepository;
import com.lifesync.lifesync.user.service.UserSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventServiceTest {
    @Mock NotificationRepository notifications;
    @Mock TaskRepository tasks;
    @Mock UserRepository users;
    @Mock UserSettingsService userSettings;
    private NotificationEventService service;

    @BeforeEach
    void setUp() {
        when(userSettings.notificationEnabled(anyLong(), any())).thenReturn(true);
        service = new NotificationEventService(notifications, tasks, users, userSettings);
    }

    @Test
    void createsOneReminderNotificationWithRelatedEntities() {
        User user = new User();
        user.setId(5L);
        com.lifesync.lifesync.task.entity.Task task = new com.lifesync.lifesync.task.entity.Task();
        task.setId(9L);
        task.setTitle("Submit assignment");
        Reminder reminder = new Reminder();
        reminder.setId(12L);
        reminder.setUser(user);
        reminder.setTask(task);
        reminder.setTitle("Assignment reminder");

        when(notifications.existsByEventKey("reminder:12")).thenReturn(false);
        assertTrue(service.reminderDue(reminder));
        when(notifications.existsByEventKey("reminder:12")).thenReturn(true);
        assertFalse(service.reminderDue(reminder));

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(saved.capture());
        assertEquals(NotificationType.REMINDER, saved.getValue().getNotificationType());
        assertEquals(user, saved.getValue().getUser());
        assertEquals(task, saved.getValue().getTask());
        assertEquals(reminder, saved.getValue().getReminder());
        assertEquals("Task reminder: Submit assignment", saved.getValue().getTitle());
        assertEquals("reminder:12", saved.getValue().getEventKey());
    }

    @Test
    void createsStandaloneReminderNotificationWithoutTaskAssociation() {
        User user = new User();
        user.setId(5L);
        Reminder reminder = new Reminder();
        reminder.setId(13L);
        reminder.setUser(user);
        reminder.setTitle("Call my friend");
        reminder.setMessage("Call after work");

        when(notifications.existsByEventKey("reminder:13")).thenReturn(false);
        assertTrue(service.reminderDue(reminder));

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(saved.capture());
        assertEquals(NotificationType.REMINDER, saved.getValue().getNotificationType());
        assertEquals("Reminder: Call my friend", saved.getValue().getTitle());
        assertEquals("Call after work", saved.getValue().getMessage());
        assertNull(saved.getValue().getTask());
        assertEquals(reminder, saved.getValue().getReminder());
    }

    @Test
    void doesNotCreateNotificationWhenTheUserDisabledItsCategory() {
        User user = new User();
        user.setId(5L);
        Reminder reminder = new Reminder();
        reminder.setId(14L);
        reminder.setUser(user);
        reminder.setTitle("Call my friend");
        reminder.setMessage("Call after work");
        when(notifications.existsByReminder_Id(14L)).thenReturn(false);
        when(userSettings.notificationEnabled(5L, NotificationType.REMINDER)).thenReturn(false);

        assertFalse(service.reminderDue(reminder));

        verify(notifications, never()).save(any(Notification.class));
    }

    @Test
    void publishesMilestoneHabitAndDailyPlanEventsWithStableDedupeKeys() {
        User user = new User();
        user.setId(5L);
        Goal goal = new Goal();
        goal.setId(4L);
        goal.setUser(user);
        goal.setTitle("Graduate");
        GoalMilestone milestone = new GoalMilestone();
        milestone.setId(7L);
        milestone.setTitle("Finish capstone");
        milestone.setCompletedAt(LocalDateTime.of(2026, 11, 13, 10, 0));
        milestone.setGoal(goal);

        Habit habit = new Habit();
        habit.setId(3L);
        habit.setUser(user);
        habit.setName("Read");
        HabitLog log = new HabitLog();
        log.setHabit(habit);
        log.setCompletionDate(LocalDate.of(2026, 11, 13));

        when(notifications.existsByEventKey(anyString())).thenReturn(false);
        when(users.findById(5L)).thenReturn(Optional.of(user));
        assertTrue(service.milestoneCompleted(milestone));
        assertTrue(service.habitCompleted(log));
        assertTrue(service.dailyPlanReady(5L, LocalDate.of(2026, 11, 13)));

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notifications, times(3)).save(saved.capture());
        assertEquals(NotificationType.MILESTONE_COMPLETED, saved.getAllValues().get(0).getNotificationType());
        assertEquals(milestone, saved.getAllValues().get(0).getMilestone());
        assertEquals(NotificationType.HABIT_COMPLETED, saved.getAllValues().get(1).getNotificationType());
        assertEquals(habit, saved.getAllValues().get(1).getHabit());
        assertEquals(NotificationType.AI_DAILY_PLAN_READY, saved.getAllValues().get(2).getNotificationType());
        assertEquals("ai-plan:5:2026-11-13", saved.getAllValues().get(2).getEventKey());
    }

    @Test
    void publishesDistinctTaskDeadlineAndOverdueEventsWithoutRepeatingThem() {
        User user = new User();
        user.setId(5L);
        Task approaching = task(20L, user, "Prepare presentation", LocalDateTime.now().plusHours(2));
        Task overdue = task(21L, user, "Submit form", LocalDateTime.now().minusHours(2));
        when(tasks.findAll(org.mockito.ArgumentMatchers.<Specification<Task>>any(), org.mockito.ArgumentMatchers.<Pageable>any()))
                .thenReturn(new PageImpl<>(java.util.List.of(approaching)))
                .thenReturn(new PageImpl<>(java.util.List.of(overdue)))
                .thenReturn(new PageImpl<>(java.util.List.of(approaching)))
                .thenReturn(new PageImpl<>(java.util.List.of(overdue)));
        when(notifications.existsByEventKey(anyString())).thenReturn(false, false, true, true);

        assertEquals(2, service.processTaskDeadlineNotifications());
        assertEquals(0, service.processTaskDeadlineNotifications());
        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notifications, times(2)).save(saved.capture());
        assertEquals(NotificationType.TASK_DEADLINE_APPROACHING, saved.getAllValues().get(0).getNotificationType());
        assertEquals(approaching, saved.getAllValues().get(0).getTask());
        assertEquals(NotificationType.TASK_OVERDUE, saved.getAllValues().get(1).getNotificationType());
        assertEquals(overdue, saved.getAllValues().get(1).getTask());
    }

    private Task task(Long id, User user, String title, LocalDateTime dueDate) {
        Task task = new Task();
        task.setId(id);
        task.setUser(user);
        task.setTitle(title);
        task.setDueDate(dueDate);
        task.setStatus(com.lifesync.lifesync.task.entity.TaskStatus.TODO);
        return task;
    }
}

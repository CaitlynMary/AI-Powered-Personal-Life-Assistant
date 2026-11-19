package com.lifesync.lifesync.notification.service;

import com.lifesync.lifesync.habit.entity.HabitLog;
import com.lifesync.lifesync.milestone.entity.GoalMilestone;
import com.lifesync.lifesync.notification.entity.Notification;
import com.lifesync.lifesync.notification.entity.NotificationType;
import com.lifesync.lifesync.notification.repository.NotificationRepository;
import com.lifesync.lifesync.reminder.entity.Reminder;
import com.lifesync.lifesync.task.entity.Task;
import com.lifesync.lifesync.task.entity.TaskStatus;
import com.lifesync.lifesync.task.repository.TaskRepository;
import com.lifesync.lifesync.user.entity.User;
import com.lifesync.lifesync.user.repository.UserRepository;
import com.lifesync.lifesync.user.service.UserSettingsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationEventService {
    private static final int PAGE_SIZE = 200;
    private static final List<TaskStatus> OPEN_TASK_STATUSES = List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS);

    private final NotificationRepository notifications;
    private final TaskRepository tasks;
    private final UserRepository users;
    private final UserSettingsService userSettings;

    public NotificationEventService(NotificationRepository notifications, TaskRepository tasks, UserRepository users,
                                    UserSettingsService userSettings) {
        this.notifications = notifications;
        this.tasks = tasks;
        this.users = users;
        this.userSettings = userSettings;
    }

    @Transactional
    public boolean reminderDue(Reminder reminder) {
        if (notifications.existsByReminder_Id(reminder.getId())) return false;
        Task task = reminder.getTask();
        String title = task == null ? "Reminder: " + reminder.getTitle() : "Task reminder: " + task.getTitle();
        String message = task == null ? reminder.getMessage() : "Your task \"" + task.getTitle() + "\" is due soon.";
        return publish("reminder:" + reminder.getId(), NotificationType.REMINDER, title, message,
                reminder.getUser(), reminder, task, null, null, null);
    }

    @Transactional
    public boolean milestoneCompleted(GoalMilestone milestone) {
        return publish("milestone:" + milestone.getId() + ":" + milestone.getCompletedAt(),
                NotificationType.MILESTONE_COMPLETED,
                "Milestone completed: " + milestone.getTitle(),
                "You completed the milestone \"" + milestone.getTitle() + "\" for goal \"" + milestone.getGoal().getTitle() + "\".",
                milestone.getGoal().getUser(), null, null, milestone.getGoal(), milestone, null);
    }

    @Transactional
    public boolean habitCompleted(HabitLog log) {
        return publish("habit:" + log.getHabit().getId() + ":" + log.getCompletionDate(),
                NotificationType.HABIT_COMPLETED,
                "Habit completed: " + log.getHabit().getName(),
                "You recorded \"" + log.getHabit().getName() + "\" as completed for " + log.getCompletionDate() + ".",
                log.getHabit().getUser(), null, null, null, null, log.getHabit());
    }

    @Transactional
    public boolean dailyPlanReady(Long userId, LocalDate date) {
        User user = users.findById(userId).orElseThrow(() -> new IllegalStateException("Plan owner was not found"));
        return publish("ai-plan:" + userId + ":" + date,
                NotificationType.AI_DAILY_PLAN_READY,
                "Your AI daily plan is ready",
                "Your plan suggestions for " + date + " are ready for review in Planner.",
                user, null, null, null, null, null);
    }

    @Transactional
    public boolean taskCompleted(Task task) {
        return publish("task-completed:" + task.getId() + ":" + task.getCompletedAt(),
                NotificationType.TASK_COMPLETED,
                "Task completed: " + task.getTitle(),
                "You completed \"" + task.getTitle() + "\".",
                task.getUser(), null, task, task.getGoal(), task.getMilestone(), null);
    }

    @Transactional
    public boolean taskMissed(Task task) {
        return publish("task-missed:" + task.getId() + ":" + task.getMissedAt(),
                NotificationType.TASK_MISSED,
                "Task marked as missed: " + task.getTitle(),
                "You marked \"" + task.getTitle() + "\" as missed.",
                task.getUser(), null, task, task.getGoal(), task.getMilestone(), null);
    }

    @Transactional
    public boolean taskRescheduled(Task task, LocalDateTime previousDeadline) {
        String newDeadline = task.getDueDate() == null ? "no deadline" : task.getDueDate().toString();
        return publish("task-rescheduled:" + task.getId() + ":" + previousDeadline + ":" + task.getDueDate(),
                NotificationType.TASK_RESCHEDULED,
                "Task rescheduled: " + task.getTitle(),
                "The deadline for \"" + task.getTitle() + "\" was changed to " + newDeadline + ".",
                task.getUser(), null, task, task.getGoal(), task.getMilestone(), null);
    }

    @Transactional
    public int processTaskDeadlineNotifications() {
        LocalDateTime now = LocalDateTime.now();
        int generated = 0;
        generated += publishTasksDueBetween(now, now.plusHours(24));
        generated += publishOverdueTasksBefore(now);
        return generated;
    }

    private int publishTasksDueBetween(LocalDateTime start, LocalDateTime end) {
        Specification<Task> specification = (root, query, cb) -> cb.and(
                root.get("status").in(OPEN_TASK_STATUSES),
                cb.isNotNull(root.get("dueDate")),
                cb.greaterThanOrEqualTo(root.get("dueDate"), start),
                cb.lessThanOrEqualTo(root.get("dueDate"), end));
        return publishTaskPages(specification, false);
    }

    private int publishOverdueTasksBefore(LocalDateTime now) {
        Specification<Task> specification = (root, query, cb) -> cb.and(
                root.get("status").in(OPEN_TASK_STATUSES),
                cb.isNotNull(root.get("dueDate")),
                cb.lessThan(root.get("dueDate"), now));
        return publishTaskPages(specification, true);
    }

    private int publishTaskPages(Specification<Task> specification, boolean overdue) {
        int generated = 0;
        Page<Task> page;
        int pageNumber = 0;
        do {
            page = tasks.findAll(specification,
                    PageRequest.of(pageNumber++, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "dueDate")));
            for (Task task : page.getContent()) {
                String deadline = task.getDueDate().toString();
                boolean created = overdue
                        ? publish("task-overdue:" + task.getId() + ":" + deadline,
                                NotificationType.TASK_OVERDUE,
                                "Task overdue: " + task.getTitle(),
                                "The deadline for \"" + task.getTitle() + "\" has passed and it is still incomplete.",
                                task.getUser(), null, task, null, null, null)
                        : publish("task-deadline:" + task.getId() + ":" + deadline,
                                NotificationType.TASK_DEADLINE_APPROACHING,
                                "Task deadline approaching: " + task.getTitle(),
                                "\"" + task.getTitle() + "\" is due within the next 24 hours.",
                                task.getUser(), null, task, null, null, null);
                if (created) generated++;
            }
        } while (page.hasNext());
        return generated;
    }

    private boolean publish(String eventKey, NotificationType type, String title, String message, User user,
                            Reminder reminder, Task task, com.lifesync.lifesync.goal.entity.Goal goal,
                            GoalMilestone milestone, com.lifesync.lifesync.habit.entity.Habit habit) {
        if (!userSettings.notificationEnabled(user.getId(), type)) return false;
        if (notifications.existsByEventKey(eventKey)) return false;
        Notification notification = new Notification();
        notification.setEventKey(eventKey);
        notification.setNotificationType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setIsRead(false);
        notification.setUser(user);
        notification.setReminder(reminder);
        notification.setTask(task);
        notification.setGoal(goal);
        notification.setMilestone(milestone);
        notification.setHabit(habit);
        notifications.save(notification);
        return true;
    }
}

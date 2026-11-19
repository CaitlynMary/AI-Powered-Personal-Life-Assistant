package com.lifesync.lifesync.ai.service;

import com.lifesync.lifesync.calendar.dto.CalendarEventResponse;
import com.lifesync.lifesync.calendar.service.CalendarEventService;
import com.lifesync.lifesync.goal.dto.GoalResponse;
import com.lifesync.lifesync.goal.entity.GoalLifecycleStatus;
import com.lifesync.lifesync.goal.service.GoalService;
import com.lifesync.lifesync.habit.dto.HabitResponse;
import com.lifesync.lifesync.habit.entity.HabitLog;
import com.lifesync.lifesync.habit.repository.HabitLogRepository;
import com.lifesync.lifesync.habit.service.HabitService;
import com.lifesync.lifesync.milestone.entity.GoalMilestone;
import com.lifesync.lifesync.milestone.entity.MilestoneLifecycleStatus;
import com.lifesync.lifesync.milestone.repository.GoalMilestoneRepository;
import com.lifesync.lifesync.note.dto.NoteResponse;
import com.lifesync.lifesync.note.service.NoteService;
import com.lifesync.lifesync.notification.dto.NotificationResponse;
import com.lifesync.lifesync.notification.service.NotificationService;
import com.lifesync.lifesync.reminder.dto.ReminderResponse;
import com.lifesync.lifesync.reminder.service.ReminderService;
import com.lifesync.lifesync.schedule.entity.ScheduleSlot;
import com.lifesync.lifesync.schedule.repository.ScheduleSlotRepository;
import com.lifesync.lifesync.task.entity.Task;
import com.lifesync.lifesync.task.entity.TaskPriority;
import com.lifesync.lifesync.task.entity.TaskStatus;
import com.lifesync.lifesync.task.repository.TaskRepository;
import com.lifesync.lifesync.task.service.TaskLifecycleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AiContextService {
    private static final int MAX_TASKS = 20;
    private static final int MAX_GOALS = 12;
    private static final int MAX_HABITS = 20;
    private static final int MAX_CONTEXT_LENGTH = 10000;

    private final TaskRepository tasks;
    private final GoalService goals;
    private final GoalMilestoneRepository milestones;
    private final HabitService habits;
    private final HabitLogRepository habitLogs;
    private final CalendarEventService calendar;
    private final ScheduleSlotRepository schedules;
    private final ReminderService reminders;
    private final NotificationService notifications;
    private final NoteService notes;
    private final TaskLifecycleService taskLifecycle;

    public AiContextService(
            TaskRepository tasks,
            GoalService goals,
            GoalMilestoneRepository milestones,
            HabitService habits,
            HabitLogRepository habitLogs,
            CalendarEventService calendar,
            ScheduleSlotRepository schedules,
            ReminderService reminders,
            NotificationService notifications,
            NoteService notes) {
        this(tasks, goals, milestones, habits, habitLogs, calendar, schedules, reminders, notifications, notes,
                new TaskLifecycleService());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AiContextService(
            TaskRepository tasks,
            GoalService goals,
            GoalMilestoneRepository milestones,
            HabitService habits,
            HabitLogRepository habitLogs,
            CalendarEventService calendar,
            ScheduleSlotRepository schedules,
            ReminderService reminders,
            NotificationService notifications,
            NoteService notes,
            TaskLifecycleService taskLifecycle) {
        this.tasks = tasks;
        this.goals = goals;
        this.milestones = milestones;
        this.habits = habits;
        this.habitLogs = habitLogs;
        this.calendar = calendar;
        this.schedules = schedules;
        this.reminders = reminders;
        this.notifications = notifications;
        this.notes = notes;
        this.taskLifecycle = taskLifecycle;
    }

    public String forQuestion(Long userId, String question) {
        String query = question == null ? "" : question.toLowerCase(Locale.ROOT);
        boolean daily = containsAny(
                query, "my day", "summarize", "summary", "what should i focus",
                "prioritize my day", "prioritise my day", "plan my day", "how productive");
        boolean week = containsAny(query, "this week", "next week", "weekly", "week's");
        StringBuilder context = new StringBuilder(3000);

        if (daily || containsAny(query, "task", "deadline", "due", "overdue", "priorit", "focus", "pending", "completed", "complete", "finish", "missed", "cancelled", "canceled")) {
            appendTasks(context, userId, query);
        }
        if (daily || containsAny(query, "goal", "milestone", "progress")) {
            appendGoals(context, userId);
        }
        if (daily || containsAny(query, "habit", "streak", "consistency")) {
            appendHabits(context, userId);
        }
        if (daily || containsAny(query, "calendar", "event", "appointment", "schedule", "available", "conflict")) {
            appendCalendarAndSchedule(context, userId, week);
        }
        if (daily || containsAny(query, "reminder")) {
            appendReminders(context, userId);
        }
        if (containsAny(query, "notification", "alert", "unread")) {
            appendNotifications(context, userId);
        }
        if (containsAny(query, "note", "journal", "diary")) {
            appendNotes(context, userId);
        }

        if (context.length() > MAX_CONTEXT_LENGTH) {
            context.setLength(MAX_CONTEXT_LENGTH);
            context.append("\n[Context was truncated to fit the safe request limit.]");
        }
        return context.toString();
    }

    private void appendTasks(StringBuilder context, Long userId, String query) {
        LocalDateTime now = LocalDateTime.now();
        long todoCount = tasks.countNotOverdueByUserAndStatus(userId, TaskStatus.TODO, now);
        long inProgressCount = tasks.countNotOverdueByUserAndStatus(userId, TaskStatus.IN_PROGRESS, now);
        long completedCount = tasks.countByUser_IdAndStatus(userId, TaskStatus.COMPLETED);
        long missedCount = tasks.countByUser_IdAndStatus(userId, TaskStatus.MISSED);
        long cancelledCount = tasks.countByUser_IdAndStatus(userId, TaskStatus.CANCELLED);
        long overdueCount = tasks.countOverdue(userId, now, List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED, TaskStatus.MISSED));
        Specification<Task> specification = (root, criteriaQuery, builder) -> {
            var userPredicate = builder.equal(root.get("user").get("id"), userId);
            if (containsAny(query, "completed", "completed task", "did i complete", "have i completed", "did i finish", "finished task")) {
                return builder.and(userPredicate, builder.equal(root.get("status"), TaskStatus.COMPLETED));
            }
            if (containsAny(query, "missed")) {
                return builder.and(userPredicate, builder.equal(root.get("status"), TaskStatus.MISSED));
            }
            if (containsAny(query, "cancelled", "canceled")) {
                return builder.and(userPredicate, builder.equal(root.get("status"), TaskStatus.CANCELLED));
            }
            var pendingPredicate = root.get("status").in(TaskStatus.TODO, TaskStatus.IN_PROGRESS);
            if (containsAny(query, "overdue")) {
                return builder.and(userPredicate, pendingPredicate, builder.lessThan(root.get("dueDate"), now));
            }
            if (containsAny(query, "deadline", "approach", "upcoming")) {
                return builder.and(userPredicate, pendingPredicate,
                        builder.greaterThanOrEqualTo(root.get("dueDate"), now),
                        builder.lessThan(root.get("dueDate"), now.plusDays(7)));
            }
            return builder.and(userPredicate, pendingPredicate);
        };
        Page<Task> page = tasks.findAll(specification,
                PageRequest.of(0, MAX_TASKS, Sort.by(Sort.Direction.ASC, "dueDate")));
        context.append("TASK RECORDS (authenticated user's data only)\n")
                .append("Counts: TODO=").append(todoCount)
                .append(", IN_PROGRESS=").append(inProgressCount)
                .append(", COMPLETED=").append(completedCount)
                .append(", MISSED=").append(missedCount)
                .append(", CANCELLED=").append(cancelledCount)
                .append(", OVERDUE=").append(overdueCount).append(".\n")
                .append("Matching records shown: ").append(page.getNumberOfElements())
                .append(" of ").append(page.getTotalElements()).append(" (maximum ").append(MAX_TASKS).append("):\n");
        for (Task task : page.getContent()) {
            context.append("- id=").append(task.getId())
                    .append("; title=\"").append(AiContentSanitizer.safeText(task.getTitle(), 150))
                    .append("\"; status=").append(taskLifecycle.statusOf(task, now))
                    .append("; priority=").append(task.getPriority())
                    .append("; due=").append(task.getDueDate())
                    .append("; estimatedMinutes=").append(task.getEstimatedDuration())
                    .append("; goalId=").append(task.getGoal() == null ? "none" : task.getGoal().getId())
                    .append("; milestoneId=").append(task.getMilestone() == null ? "none" : task.getMilestone().getId())
                    .append('\n');
        }
    }

    private void appendGoals(StringBuilder context, Long userId) {
        List<GoalResponse> records = goals.getAll(userId).stream()
                .filter(goal -> goal.getStatus() != GoalLifecycleStatus.CANCELLED)
                .limit(MAX_GOALS)
                .toList();
        context.append("GOALS AND MILESTONES (authenticated user's data only; up to ")
                .append(MAX_GOALS).append(" goals):\n");
        if (records.isEmpty()) context.append("- No goal records were returned.\n");
        for (GoalResponse goal : records) {
            context.append("- title=\"").append(AiContentSanitizer.safeText(goal.getTitle(), 150))
                    .append("\"; status=").append(goal.getStatus())
                    .append("; progress=").append(goal.getProgressPercentage())
                    .append("%; target=").append(goal.getTargetDate()).append('\n');
            milestones.findAllByGoal_IdOrderByTargetDateAsc(goal.getId()).stream()
                    .filter(milestone -> !milestone.getStatus().name().equals("CANCELLED"))
                    .limit(10)
                    .forEach(milestone -> appendMilestone(context, milestone));
        }
    }

    private void appendMilestone(StringBuilder context, GoalMilestone milestone) {
        context.append("  milestone=\"").append(AiContentSanitizer.safeText(milestone.getTitle(), 120))
                .append("\"; status=").append(MilestoneLifecycleStatus.of(milestone, LocalDate.now()))
                .append("; target=").append(milestone.getTargetDate()).append('\n');
    }

    private void appendHabits(StringBuilder context, Long userId) {
        List<HabitResponse> active = habits.getAll(userId).stream()
                .filter(habit -> Boolean.TRUE.equals(habit.getActive()))
                .limit(MAX_HABITS)
                .toList();
        LocalDate today = LocalDate.now();
        List<HabitLog> logs = habitLogs.findAllByHabit_User_IdAndCompletionDateBetween(
                userId, today.minusDays(6), today);
        Set<Long> activeIds = new HashSet<>();
        active.forEach(habit -> activeIds.add(habit.getId()));
        context.append("ACTIVE HABITS (recent completion logs; absence of a log is unknown, not a miss):\n");
        if (active.isEmpty()) context.append("- No active habits were returned.\n");
        for (HabitResponse habit : active) {
            long completedDays = logs.stream()
                    .filter(log -> log.getHabit().getId().equals(habit.getId()))
                    .filter(log -> Boolean.TRUE.equals(log.getCompleted()))
                    .count();
            context.append("- name=\"").append(AiContentSanitizer.safeText(habit.getName(), 120))
                    .append("\"; frequency=").append(habit.getFrequency())
                    .append("; targetDaysPerWeek=").append(habit.getTargetDaysPerWeek())
                    .append("; completedLogDaysInLast7=").append(completedDays)
                    .append("; completionToday=");
            logs.stream()
                    .filter(log -> activeIds.contains(log.getHabit().getId()))
                    .filter(log -> log.getHabit().getId().equals(habit.getId()))
                    .filter(log -> log.getCompletionDate().equals(today))
                    .findFirst()
                    .ifPresentOrElse(
                            log -> context.append(Boolean.TRUE.equals(log.getCompleted())),
                            () -> context.append("unknown (no log)"));
            context.append('\n');
        }
    }

    private void appendCalendarAndSchedule(StringBuilder context, Long userId, boolean week) {
        LocalDate today = LocalDate.now();
        LocalDate start = week ? today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) : today;
        LocalDate end = week ? start.plusDays(7) : start.plusDays(1);
        List<CalendarEventResponse> events = week
                ? calendar.week(userId, start)
                : calendar.day(userId, start);
        List<ScheduleSlot> slots = schedules.findOverlappingRange(
                userId, start.atStartOfDay(), end.atStartOfDay());
        context.append(week ? "CALENDAR EVENTS THIS WEEK:\n" : "CALENDAR EVENTS TODAY:\n");
        if (events.isEmpty()) context.append("- No calendar events were returned for this date range.\n");
        events.stream().limit(20).forEach(event -> context.append("- title=\"")
                .append(AiContentSanitizer.safeText(event.getTitle(), 150))
                .append("\"; type=").append(event.getEventType())
                .append("; start=").append(event.getStartDateTime())
                .append("; end=").append(event.getEndDateTime()).append('\n'));
        context.append(week ? "SCHEDULE SLOTS THIS WEEK:\n" : "SCHEDULE SLOTS TODAY:\n");
        if (slots.isEmpty()) context.append("- No schedule slots were returned for this date range.\n");
        slots.stream().limit(20).forEach(slot -> context.append("- title=\"")
                .append(AiContentSanitizer.safeText(slot.getTitle(), 150))
                .append("\"; start=").append(slot.getStartDateTime())
                .append("; end=").append(slot.getEndDateTime()).append('\n'));
    }

    private void appendReminders(StringBuilder context, Long userId) {
        List<ReminderResponse> records = reminders.upcoming(userId, 0, 10).getContent();
        context.append("UPCOMING PENDING REMINDERS (maximum 10):\n");
        if (records.isEmpty()) context.append("- No upcoming pending reminders were returned.\n");
        records.forEach(reminder -> context.append("- title=\"")
                .append(AiContentSanitizer.safeText(reminder.getTitle(), 150))
                .append("\"; type=").append(reminder.getReminderType())
                .append("; due=").append(reminder.getReminderDateTime())
                .append("; status=").append(reminder.getStatus()).append('\n'));
    }

    private void appendNotifications(StringBuilder context, Long userId) {
        List<NotificationResponse> records = notifications.getAll(userId, 0, 10, true).getContent();
        context.append("UNREAD NOTIFICATIONS (maximum 10):\n");
        if (records.isEmpty()) context.append("- No unread notifications were returned.\n");
        records.forEach(notification -> context.append("- title=\"")
                .append(AiContentSanitizer.safeText(notification.getTitle(), 150))
                .append("\"; type=").append(notification.getNotificationType())
                .append("; created=").append(notification.getCreatedAt()).append('\n'));
    }

    private void appendNotes(StringBuilder context, Long userId) {
        List<NoteResponse> records = notes.getAll(userId, 0, 5, "", "updatedAt", "DESC").getContent();
        context.append("RECENT NOTE CONTENT (included because the question explicitly mentions notes/journaling; maximum 5):\n");
        if (records.isEmpty()) context.append("- No note records were returned.\n");
        records.forEach(note -> context.append("- type=").append(note.getNoteType())
                .append("; date=").append(note.getJournalDate())
                .append("; title=\"").append(AiContentSanitizer.safeText(note.getTitle(), 100))
                .append("\"; content=\"").append(AiContentSanitizer.safeText(note.getContent(), 600))
                .append("\"\n"));
    }

    private boolean containsAny(String value, String... terms) {
        for (String term : terms) {
            if (value.contains(term)) return true;
        }
        return false;
    }
}

package com.lifesync.lifesync.ai.service;

import com.lifesync.lifesync.ai.dto.DailyBriefingResponse;
import com.lifesync.lifesync.ai.service.AiFailureDiagnostics;
import com.lifesync.lifesync.calendar.dto.CalendarEventResponse;
import com.lifesync.lifesync.calendar.service.CalendarEventService;
import com.lifesync.lifesync.habit.dto.HabitStatisticsResponse;
import com.lifesync.lifesync.habit.service.HabitService;
import com.lifesync.lifesync.reminder.dto.ReminderPageResponse;
import com.lifesync.lifesync.reminder.service.ReminderService;
import com.lifesync.lifesync.task.dto.TaskPageResponse;
import com.lifesync.lifesync.task.dto.TaskResponse;
import com.lifesync.lifesync.task.dto.TaskStatisticsResponse;
import com.lifesync.lifesync.task.entity.TaskLifecycleStatus;
import com.lifesync.lifesync.task.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class DailyBriefingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DailyBriefingService.class);
    private static final int MAX_UPCOMING_DEADLINES = 8;
    private static final int MAX_EVENTS = 8;
    private static final int MAX_REMINDERS = 5;

    private final TaskService tasks;
    private final CalendarEventService calendar;
    private final ReminderService reminders;
    private final HabitService habits;

    public DailyBriefingService(
            TaskService tasks,
            CalendarEventService calendar,
            ReminderService reminders,
            HabitService habits) {
        this.tasks = tasks;
        this.calendar = calendar;
        this.reminders = reminders;
        this.habits = habits;
    }

    public DailyBriefingResponse today(Long userId) {
        String operation = "task statistics";
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDate today = now.toLocalDate();
            TaskStatisticsResponse taskStats = tasks.statistics(userId);
            List<TaskResponse> deadlines = new ArrayList<>();
            for (TaskLifecycleStatus status : List.of(TaskLifecycleStatus.TODO, TaskLifecycleStatus.IN_PROGRESS)) {
                operation = status + " deadline query";
                TaskPageResponse page = tasks.getAll(
                        userId, 0, 100, "dueDate", "ASC", status, null, null,
                        now, now.plusDays(7), null);
                deadlines.addAll(page.getContent());
            }
            operation = "habit statistics";
            HabitStatisticsResponse habitStats = habits.statistics(userId);
            operation = "upcoming reminders";
            ReminderPageResponse reminderPage = reminders.upcoming(userId, 0, MAX_REMINDERS);
            operation = "today's calendar events";
            List<CalendarEventResponse> events = calendar.day(userId, today);

            return new DailyBriefingResponse(
                    today,
                    taskStats.getPendingTasks() + taskStats.getInProgressTasks(),
                    taskStats.getOverdueTasks(),
                    taskStats.getMissedTasks(),
                    taskStats.getTasksDueToday(),
                    taskStats.getHighPriorityTasks(),
                    deadlines.stream()
                            .sorted(Comparator.comparing(TaskResponse::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                            .limit(MAX_UPCOMING_DEADLINES)
                            .toList(),
                    events.stream().limit(MAX_EVENTS).toList(),
                    reminderPage.getTotalElements(),
                    reminderPage.getContent(),
                    habitStats.getActiveHabits(),
                    habitStats.getCompletionsThisWeek(),
                    habitStats.getWeeklyConsistencyPercentage());
        } catch (RuntimeException ex) {
            LOGGER.error(
                    "Daily briefing failed (operation={}, exceptionType={}, rootCauseType={}, message={})",
                    operation,
                    ex.getClass().getSimpleName(),
                    AiFailureDiagnostics.rootCauseType(ex),
                    AiFailureDiagnostics.safeMessage(ex));
            throw ex;
        }
    }
}

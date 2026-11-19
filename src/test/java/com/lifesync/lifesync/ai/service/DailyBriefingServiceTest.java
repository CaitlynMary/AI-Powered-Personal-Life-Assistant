package com.lifesync.lifesync.ai.service;

import com.lifesync.lifesync.calendar.dto.CalendarEventResponse;
import com.lifesync.lifesync.calendar.service.CalendarEventService;
import com.lifesync.lifesync.habit.dto.HabitStatisticsResponse;
import com.lifesync.lifesync.habit.service.HabitService;
import com.lifesync.lifesync.reminder.dto.ReminderPageResponse;
import com.lifesync.lifesync.reminder.service.ReminderService;
import com.lifesync.lifesync.task.dto.TaskPageResponse;
import com.lifesync.lifesync.task.dto.TaskStatisticsResponse;
import com.lifesync.lifesync.task.entity.TaskLifecycleStatus;
import com.lifesync.lifesync.task.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DailyBriefingServiceTest {
    private TaskService tasks;
    private CalendarEventService calendar;
    private ReminderService reminders;
    private HabitService habits;
    private DailyBriefingService service;

    @BeforeEach
    void setUp() {
        tasks = mock(TaskService.class);
        calendar = mock(CalendarEventService.class);
        reminders = mock(ReminderService.class);
        habits = mock(HabitService.class);
        service = new DailyBriefingService(tasks, calendar, reminders, habits);
        when(tasks.statistics(27L)).thenReturn(new TaskStatisticsResponse(12, 8, 3, 2, 4, 1, 1, 2, 3, 5));
        when(tasks.getAll(eq(27L), eq(0), eq(100), eq("dueDate"), eq("ASC"),
                eq(TaskLifecycleStatus.TODO), isNull(), isNull(), any(), any(), isNull()))
                .thenReturn(new TaskPageResponse(List.of(), 0, 100, 0, 0, true));
        when(tasks.getAll(eq(27L), eq(0), eq(100), eq("dueDate"), eq("ASC"),
                eq(TaskLifecycleStatus.IN_PROGRESS), isNull(), isNull(), any(), any(), isNull()))
                .thenReturn(new TaskPageResponse(List.of(), 0, 100, 0, 0, true));
        when(calendar.day(eq(27L), any())).thenReturn(List.of());
        when(reminders.upcoming(27L, 0, 5)).thenReturn(new ReminderPageResponse(List.of(), 0, 5, 7, 2, false));
        when(habits.statistics(27L)).thenReturn(new HabitStatisticsResponse(4, 3, 15, 5, 11, 72.5, 68.0));
    }

    @Test
    void assemblesBriefingFromAuthenticatedTaskCalendarReminderAndHabitServices() {
        var briefing = service.today(27L);

        assertEquals(5, briefing.getPendingTasks());
        assertEquals(2, briefing.getOverdueTasks());
        assertEquals(1, briefing.getMissedTasks());
        assertEquals(3, briefing.getTasksDueToday());
        assertEquals(7, briefing.getUpcomingReminderCount());
        assertEquals(3, briefing.getActiveHabits());
        assertEquals(5, briefing.getHabitCompletionsThisWeek());
        assertEquals(72.5, briefing.getWeeklyHabitConsistencyPercentage());
        verify(tasks).statistics(27L);
        verify(tasks).getAll(eq(27L), eq(0), eq(100), eq("dueDate"), eq("ASC"),
                eq(TaskLifecycleStatus.TODO), isNull(), isNull(), any(), any(), isNull());
        verify(tasks).getAll(eq(27L), eq(0), eq(100), eq("dueDate"), eq("ASC"),
                eq(TaskLifecycleStatus.IN_PROGRESS), isNull(), isNull(), any(), any(), isNull());
        verify(calendar).day(eq(27L), any());
        verify(reminders).upcoming(27L, 0, 5);
        verify(habits).statistics(27L);
    }
}

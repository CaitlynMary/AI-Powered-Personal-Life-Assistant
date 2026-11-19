package com.lifesync.lifesync.ai.dto;

import com.lifesync.lifesync.calendar.dto.CalendarEventResponse;
import com.lifesync.lifesync.habit.dto.HabitStatisticsResponse;
import com.lifesync.lifesync.reminder.dto.ReminderResponse;
import com.lifesync.lifesync.task.dto.TaskResponse;

import java.time.LocalDate;
import java.util.List;

public class DailyBriefingResponse {
    private LocalDate date;
    private long pendingTasks;
    private long overdueTasks;
    private long missedTasks;
    private long tasksDueToday;
    private long highPriorityTasks;
    private List<TaskResponse> upcomingDeadlines;
    private List<CalendarEventResponse> eventsToday;
    private long upcomingReminderCount;
    private List<ReminderResponse> upcomingReminders;
    private long activeHabits;
    private long habitCompletionsThisWeek;
    private double weeklyHabitConsistencyPercentage;

    public DailyBriefingResponse() { }

    public DailyBriefingResponse(
            LocalDate date,
            long pendingTasks,
            long overdueTasks,
            long missedTasks,
            long tasksDueToday,
            long highPriorityTasks,
            List<TaskResponse> upcomingDeadlines,
            List<CalendarEventResponse> eventsToday,
            long upcomingReminderCount,
            List<ReminderResponse> upcomingReminders,
            long activeHabits,
            long habitCompletionsThisWeek,
            double weeklyHabitConsistencyPercentage) {
        this.date = date;
        this.pendingTasks = pendingTasks;
        this.overdueTasks = overdueTasks;
        this.missedTasks = missedTasks;
        this.tasksDueToday = tasksDueToday;
        this.highPriorityTasks = highPriorityTasks;
        this.upcomingDeadlines = upcomingDeadlines;
        this.eventsToday = eventsToday;
        this.upcomingReminderCount = upcomingReminderCount;
        this.upcomingReminders = upcomingReminders;
        this.activeHabits = activeHabits;
        this.habitCompletionsThisWeek = habitCompletionsThisWeek;
        this.weeklyHabitConsistencyPercentage = weeklyHabitConsistencyPercentage;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public long getPendingTasks() { return pendingTasks; }
    public void setPendingTasks(long pendingTasks) { this.pendingTasks = pendingTasks; }
    public long getOverdueTasks() { return overdueTasks; }
    public void setOverdueTasks(long overdueTasks) { this.overdueTasks = overdueTasks; }
    public long getMissedTasks() { return missedTasks; }
    public void setMissedTasks(long missedTasks) { this.missedTasks = missedTasks; }
    public long getTasksDueToday() { return tasksDueToday; }
    public void setTasksDueToday(long tasksDueToday) { this.tasksDueToday = tasksDueToday; }
    public long getHighPriorityTasks() { return highPriorityTasks; }
    public void setHighPriorityTasks(long highPriorityTasks) { this.highPriorityTasks = highPriorityTasks; }
    public List<TaskResponse> getUpcomingDeadlines() { return upcomingDeadlines; }
    public void setUpcomingDeadlines(List<TaskResponse> upcomingDeadlines) { this.upcomingDeadlines = upcomingDeadlines; }
    public List<CalendarEventResponse> getEventsToday() { return eventsToday; }
    public void setEventsToday(List<CalendarEventResponse> eventsToday) { this.eventsToday = eventsToday; }
    public long getUpcomingReminderCount() { return upcomingReminderCount; }
    public void setUpcomingReminderCount(long upcomingReminderCount) { this.upcomingReminderCount = upcomingReminderCount; }
    public List<ReminderResponse> getUpcomingReminders() { return upcomingReminders; }
    public void setUpcomingReminders(List<ReminderResponse> upcomingReminders) { this.upcomingReminders = upcomingReminders; }
    public long getActiveHabits() { return activeHabits; }
    public void setActiveHabits(long activeHabits) { this.activeHabits = activeHabits; }
    public long getHabitCompletionsThisWeek() { return habitCompletionsThisWeek; }
    public void setHabitCompletionsThisWeek(long habitCompletionsThisWeek) { this.habitCompletionsThisWeek = habitCompletionsThisWeek; }
    public double getWeeklyHabitConsistencyPercentage() { return weeklyHabitConsistencyPercentage; }
    public void setWeeklyHabitConsistencyPercentage(double weeklyHabitConsistencyPercentage) { this.weeklyHabitConsistencyPercentage = weeklyHabitConsistencyPercentage; }
}

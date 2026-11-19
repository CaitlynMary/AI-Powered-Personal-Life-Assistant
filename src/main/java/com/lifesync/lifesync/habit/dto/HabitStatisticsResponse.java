package com.lifesync.lifesync.habit.dto;
public class HabitStatisticsResponse {
    private long totalHabits; private long activeHabits; private long totalCompletedDays;
    private long completionsThisWeek; private long completionsThisMonth;
    private double weeklyConsistencyPercentage; private double monthlyConsistencyPercentage;
    public HabitStatisticsResponse() { }
    public HabitStatisticsResponse(long totalHabits,long activeHabits,long totalCompletedDays,long completionsThisWeek,long completionsThisMonth,double weeklyConsistencyPercentage,double monthlyConsistencyPercentage){this.totalHabits=totalHabits;this.activeHabits=activeHabits;this.totalCompletedDays=totalCompletedDays;this.completionsThisWeek=completionsThisWeek;this.completionsThisMonth=completionsThisMonth;this.weeklyConsistencyPercentage=weeklyConsistencyPercentage;this.monthlyConsistencyPercentage=monthlyConsistencyPercentage;}
    public long getTotalHabits(){return totalHabits;} public void setTotalHabits(long v){totalHabits=v;} public long getActiveHabits(){return activeHabits;} public void setActiveHabits(long v){activeHabits=v;}
    public long getTotalCompletedDays(){return totalCompletedDays;} public void setTotalCompletedDays(long v){totalCompletedDays=v;} public long getCompletionsThisWeek(){return completionsThisWeek;} public void setCompletionsThisWeek(long v){completionsThisWeek=v;}
    public long getCompletionsThisMonth(){return completionsThisMonth;} public void setCompletionsThisMonth(long v){completionsThisMonth=v;}
    public double getWeeklyConsistencyPercentage(){return weeklyConsistencyPercentage;} public void setWeeklyConsistencyPercentage(double v){weeklyConsistencyPercentage=v;}
    public double getMonthlyConsistencyPercentage(){return monthlyConsistencyPercentage;} public void setMonthlyConsistencyPercentage(double v){monthlyConsistencyPercentage=v;}
}

package com.lifesync.lifesync.habit.dto;
public class HabitStreakResponse {
    private int currentStreak; private int longestStreak; private long totalCompletedDays; private double completionPercentage; private String periodUnit;
    public HabitStreakResponse() { }
    public HabitStreakResponse(int currentStreak,int longestStreak,long totalCompletedDays,double completionPercentage,String periodUnit){this.currentStreak=currentStreak;this.longestStreak=longestStreak;this.totalCompletedDays=totalCompletedDays;this.completionPercentage=completionPercentage;this.periodUnit=periodUnit;}
    public int getCurrentStreak(){return currentStreak;} public void setCurrentStreak(int v){currentStreak=v;} public int getLongestStreak(){return longestStreak;} public void setLongestStreak(int v){longestStreak=v;}
    public long getTotalCompletedDays(){return totalCompletedDays;} public void setTotalCompletedDays(long v){totalCompletedDays=v;} public double getCompletionPercentage(){return completionPercentage;} public void setCompletionPercentage(double v){completionPercentage=v;}
    public String getPeriodUnit(){return periodUnit;} public void setPeriodUnit(String v){periodUnit=v;}
}

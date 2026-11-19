package com.lifesync.lifesync.habit.dto;
import java.util.List;
public class HabitCalendarResponse {
    private Long habitId; private int year; private int month; private long completedDays; private long eligibleDays; private double completionPercentage; private List<HabitCalendarDay> days;
    public HabitCalendarResponse() { }
    public HabitCalendarResponse(Long habitId,int year,int month,long completedDays,long eligibleDays,double completionPercentage,List<HabitCalendarDay> days){this.habitId=habitId;this.year=year;this.month=month;this.completedDays=completedDays;this.eligibleDays=eligibleDays;this.completionPercentage=completionPercentage;this.days=days;}
    public Long getHabitId(){return habitId;} public void setHabitId(Long v){habitId=v;} public int getYear(){return year;} public void setYear(int v){year=v;} public int getMonth(){return month;} public void setMonth(int v){month=v;}
    public long getCompletedDays(){return completedDays;} public void setCompletedDays(long v){completedDays=v;} public long getEligibleDays(){return eligibleDays;} public void setEligibleDays(long v){eligibleDays=v;}
    public double getCompletionPercentage(){return completionPercentage;} public void setCompletionPercentage(double v){completionPercentage=v;} public List<HabitCalendarDay> getDays(){return days;} public void setDays(List<HabitCalendarDay> v){days=v;}
}

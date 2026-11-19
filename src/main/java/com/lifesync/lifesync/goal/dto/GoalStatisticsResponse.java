package com.lifesync.lifesync.goal.dto;
public class GoalStatisticsResponse {
    private long totalGoals; private long notStarted; private long inProgress; private long completed; private long cancelled; private long overdue; private double averageProgress;
    public GoalStatisticsResponse() { }
    public GoalStatisticsResponse(long totalGoals,long notStarted,long inProgress,long completed,long cancelled,long overdue,double averageProgress){this.totalGoals=totalGoals;this.notStarted=notStarted;this.inProgress=inProgress;this.completed=completed;this.cancelled=cancelled;this.overdue=overdue;this.averageProgress=averageProgress;}
    public long getTotalGoals(){return totalGoals;} public void setTotalGoals(long v){totalGoals=v;}
    public long getNotStarted(){return notStarted;} public void setNotStarted(long v){notStarted=v;} public long getInProgress(){return inProgress;} public void setInProgress(long v){inProgress=v;}
    public long getCompleted(){return completed;} public void setCompleted(long v){completed=v;} public long getCancelled(){return cancelled;} public void setCancelled(long v){cancelled=v;}
    public long getOverdue(){return overdue;} public void setOverdue(long v){overdue=v;} public double getAverageProgress(){return averageProgress;} public void setAverageProgress(double v){averageProgress=v;}
}

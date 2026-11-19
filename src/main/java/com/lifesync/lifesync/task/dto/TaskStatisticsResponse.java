package com.lifesync.lifesync.task.dto;
public class TaskStatisticsResponse {
    private long totalTasks; private long activeTasks; private long pendingTasks; private long inProgressTasks;
    private long completedTasks; private long missedTasks; private long cancelledTasks;
    private long overdueTasks; private long tasksDueToday; private long highPriorityTasks;
    public TaskStatisticsResponse() { }
    public TaskStatisticsResponse(long totalTasks,long pendingTasks,long inProgressTasks,long completedTasks,long cancelledTasks,long overdueTasks,long tasksDueToday,long highPriorityTasks) {
        this.totalTasks=totalTasks;this.pendingTasks=pendingTasks;this.inProgressTasks=inProgressTasks;this.completedTasks=completedTasks;this.cancelledTasks=cancelledTasks;this.overdueTasks=overdueTasks;this.tasksDueToday=tasksDueToday;this.highPriorityTasks=highPriorityTasks;
    }
    public TaskStatisticsResponse(long totalTasks,long activeTasks,long pendingTasks,long inProgressTasks,long completedTasks,long missedTasks,long cancelledTasks,long overdueTasks,long tasksDueToday,long highPriorityTasks) {
        this(totalTasks,pendingTasks,inProgressTasks,completedTasks,cancelledTasks,overdueTasks,tasksDueToday,highPriorityTasks);
        this.activeTasks=activeTasks;this.missedTasks=missedTasks;
    }
    public long getTotalTasks(){return totalTasks;} public void setTotalTasks(long value){totalTasks=value;}
    public long getActiveTasks(){return activeTasks;} public void setActiveTasks(long value){activeTasks=value;}
    public long getPendingTasks(){return pendingTasks;} public void setPendingTasks(long value){pendingTasks=value;}
    public long getInProgressTasks(){return inProgressTasks;} public void setInProgressTasks(long value){inProgressTasks=value;}
    public long getCompletedTasks(){return completedTasks;} public void setCompletedTasks(long value){completedTasks=value;}
    public long getMissedTasks(){return missedTasks;} public void setMissedTasks(long value){missedTasks=value;}
    public long getCancelledTasks(){return cancelledTasks;} public void setCancelledTasks(long value){cancelledTasks=value;}
    public long getOverdueTasks(){return overdueTasks;} public void setOverdueTasks(long value){overdueTasks=value;}
    public long getTasksDueToday(){return tasksDueToday;} public void setTasksDueToday(long value){tasksDueToday=value;}
    public long getHighPriorityTasks(){return highPriorityTasks;} public void setHighPriorityTasks(long value){highPriorityTasks=value;}
}

package com.lifesync.lifesync.ai.dto;

import com.lifesync.lifesync.task.entity.TaskPriority;
import java.time.LocalTime;

public class DailyPlanSuggestion {
    private Long taskId;
    private String taskTitle;
    private LocalTime startTime;
    private LocalTime endTime;
    private TaskPriority priority;
    private String rationale;
    public DailyPlanSuggestion() { }
    public DailyPlanSuggestion(Long taskId, String taskTitle, LocalTime startTime, LocalTime endTime, TaskPriority priority, String rationale) {
        this.taskId=taskId; this.taskTitle=taskTitle; this.startTime=startTime; this.endTime=endTime; this.priority=priority; this.rationale=rationale;
    }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId=taskId; }
    public String getTaskTitle() { return taskTitle; }
    public void setTaskTitle(String taskTitle) { this.taskTitle=taskTitle; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime=startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime=endTime; }
    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority=priority; }
    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale=rationale; }
}

package com.lifesync.lifesync.schedule.dto;

import java.time.LocalDateTime;

public class ScheduleSlotResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private Long taskId;
    private String taskTitle;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ScheduleSlotResponse() { }
    public ScheduleSlotResponse(Long id, String title, String description, LocalDateTime startDateTime, LocalDateTime endDateTime,
                                Long taskId, String taskTitle, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id=id; this.title=title; this.description=description; this.startDateTime=startDateTime; this.endDateTime=endDateTime;
        this.taskId=taskId; this.taskTitle=taskTitle; this.createdAt=createdAt; this.updatedAt=updatedAt;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getStartDateTime() { return startDateTime; }
    public void setStartDateTime(LocalDateTime startDateTime) { this.startDateTime = startDateTime; }
    public LocalDateTime getEndDateTime() { return endDateTime; }
    public void setEndDateTime(LocalDateTime endDateTime) { this.endDateTime = endDateTime; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getTaskTitle() { return taskTitle; }
    public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

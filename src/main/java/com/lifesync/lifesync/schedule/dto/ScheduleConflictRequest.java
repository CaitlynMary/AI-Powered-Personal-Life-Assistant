package com.lifesync.lifesync.schedule.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

public class ScheduleConflictRequest {
    @NotNull private LocalDateTime startDateTime;
    @NotNull private LocalDateTime endDateTime;
    @Positive private Long taskId;
    public ScheduleConflictRequest() { }
    public ScheduleConflictRequest(LocalDateTime startDateTime, LocalDateTime endDateTime, Long taskId) { this.startDateTime=startDateTime; this.endDateTime=endDateTime; this.taskId=taskId; }
    public LocalDateTime getStartDateTime() { return startDateTime; }
    public void setStartDateTime(LocalDateTime startDateTime) { this.startDateTime=startDateTime; }
    public LocalDateTime getEndDateTime() { return endDateTime; }
    public void setEndDateTime(LocalDateTime endDateTime) { this.endDateTime=endDateTime; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId=taskId; }
}

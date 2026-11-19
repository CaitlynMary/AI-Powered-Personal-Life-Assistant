package com.lifesync.lifesync.schedule.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalTime;

public class DailyScheduleApprovalItem {
    @Positive
    @NotNull
    private Long taskId;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    public DailyScheduleApprovalItem() { }

    public DailyScheduleApprovalItem(Long taskId, LocalTime startTime, LocalTime endTime) {
        this.taskId = taskId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
}

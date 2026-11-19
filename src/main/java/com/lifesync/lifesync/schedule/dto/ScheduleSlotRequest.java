package com.lifesync.lifesync.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class ScheduleSlotRequest {
    @NotBlank @Size(max = 150) private String title;
    @Size(max = 2000) private String description;
    @NotNull private LocalDateTime startDateTime;
    @NotNull private LocalDateTime endDateTime;
    @Positive private Long taskId;

    public ScheduleSlotRequest() { }
    public ScheduleSlotRequest(String title, String description, LocalDateTime startDateTime, LocalDateTime endDateTime, Long taskId) {
        this.title = title; this.description = description; this.startDateTime = startDateTime; this.endDateTime = endDateTime; this.taskId = taskId;
    }
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
}

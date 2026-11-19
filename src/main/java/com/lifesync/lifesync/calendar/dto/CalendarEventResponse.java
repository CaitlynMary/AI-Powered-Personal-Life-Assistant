package com.lifesync.lifesync.calendar.dto;

import com.lifesync.lifesync.calendar.entity.CalendarEventType;
import com.lifesync.lifesync.calendar.entity.CalendarRecurrenceType;
import java.time.LocalDateTime;

public class CalendarEventResponse {
    private Long id;
    private String title;
    private String description;
    private CalendarEventType eventType;
    private Boolean allDay;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private CalendarRecurrenceType recurrenceType;
    private Long taskId;
    private String taskTitle;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CalendarEventResponse() { }
    public CalendarEventResponse(Long id, String title, String description, CalendarEventType eventType, Boolean allDay,
                                 LocalDateTime startDateTime, LocalDateTime endDateTime, CalendarRecurrenceType recurrenceType,
                                 Long taskId, String taskTitle, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id=id; this.title=title; this.description=description; this.eventType=eventType; this.allDay=allDay;
        this.startDateTime=startDateTime; this.endDateTime=endDateTime; this.recurrenceType=recurrenceType;
        this.taskId=taskId; this.taskTitle=taskTitle; this.createdAt=createdAt; this.updatedAt=updatedAt;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id=id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title=title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description=description; }
    public CalendarEventType getEventType() { return eventType; }
    public void setEventType(CalendarEventType eventType) { this.eventType=eventType; }
    public Boolean getAllDay() { return allDay; }
    public void setAllDay(Boolean allDay) { this.allDay=allDay; }
    public LocalDateTime getStartDateTime() { return startDateTime; }
    public void setStartDateTime(LocalDateTime startDateTime) { this.startDateTime=startDateTime; }
    public LocalDateTime getEndDateTime() { return endDateTime; }
    public void setEndDateTime(LocalDateTime endDateTime) { this.endDateTime=endDateTime; }
    public CalendarRecurrenceType getRecurrenceType() { return recurrenceType; }
    public void setRecurrenceType(CalendarRecurrenceType recurrenceType) { this.recurrenceType=recurrenceType; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId=taskId; }
    public String getTaskTitle() { return taskTitle; }
    public void setTaskTitle(String taskTitle) { this.taskTitle=taskTitle; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt=createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt=updatedAt; }
}

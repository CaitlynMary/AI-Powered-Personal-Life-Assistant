package com.lifesync.lifesync.reminder.dto;
import com.lifesync.lifesync.reminder.entity.ReminderStatus;
import com.lifesync.lifesync.reminder.entity.ReminderType;
import java.time.LocalDateTime;
public class ReminderResponse {
    private Long id; private String title; private String message; private LocalDateTime reminderDateTime; private ReminderStatus status; private ReminderType reminderType; private Long taskId; private String taskTitle; private Integer deadlineOffsetMinutes; private LocalDateTime createdAt; private LocalDateTime updatedAt;
    public ReminderResponse() { }
    public ReminderResponse(Long id,String title,String message,LocalDateTime reminderDateTime,ReminderStatus status,ReminderType reminderType,Long taskId,String taskTitle,Integer deadlineOffsetMinutes,LocalDateTime createdAt,LocalDateTime updatedAt){this.id=id;this.title=title;this.message=message;this.reminderDateTime=reminderDateTime;this.status=status;this.reminderType=reminderType;this.taskId=taskId;this.taskTitle=taskTitle;this.deadlineOffsetMinutes=deadlineOffsetMinutes;this.createdAt=createdAt;this.updatedAt=updatedAt;}
    public Long getId(){return id;} public void setId(Long v){id=v;} public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getMessage(){return message;} public void setMessage(String v){message=v;} public LocalDateTime getReminderDateTime(){return reminderDateTime;} public void setReminderDateTime(LocalDateTime v){reminderDateTime=v;}
    public ReminderStatus getStatus(){return status;} public void setStatus(ReminderStatus v){status=v;} public ReminderType getReminderType(){return reminderType;} public void setReminderType(ReminderType v){reminderType=v;}
    public Integer getDeadlineOffsetMinutes(){return deadlineOffsetMinutes;} public void setDeadlineOffsetMinutes(Integer v){deadlineOffsetMinutes=v;}
    public Long getTaskId(){return taskId;} public void setTaskId(Long v){taskId=v;} public String getTaskTitle(){return taskTitle;} public void setTaskTitle(String v){taskTitle=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;} public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
}

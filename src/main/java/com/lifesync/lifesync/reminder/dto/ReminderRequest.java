package com.lifesync.lifesync.reminder.dto;
import com.lifesync.lifesync.reminder.entity.ReminderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
public class ReminderRequest {
    @NotBlank @Size(max=200) private String title;
    @NotBlank @Size(max=2000) private String message;
    @NotNull private LocalDateTime reminderDateTime;
    @NotNull private ReminderType reminderType;
    private Long taskId;
    private Integer deadlineOffsetMinutes;
    public ReminderRequest() { }
    public ReminderRequest(String title,String message,LocalDateTime reminderDateTime,ReminderType reminderType){this(title,message,reminderDateTime,reminderType,null);} 
    public ReminderRequest(String title,String message,LocalDateTime reminderDateTime,ReminderType reminderType,Long taskId){this.title=title;this.message=message;this.reminderDateTime=reminderDateTime;this.reminderType=reminderType;this.taskId=taskId;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getMessage(){return message;} public void setMessage(String v){message=v;}
    public LocalDateTime getReminderDateTime(){return reminderDateTime;} public void setReminderDateTime(LocalDateTime v){reminderDateTime=v;}
    public ReminderType getReminderType(){return reminderType;} public void setReminderType(ReminderType v){reminderType=v;}
    public Long getTaskId(){return taskId;} public void setTaskId(Long v){taskId=v;}
    public Integer getDeadlineOffsetMinutes(){return deadlineOffsetMinutes;} public void setDeadlineOffsetMinutes(Integer v){deadlineOffsetMinutes=v;}
}

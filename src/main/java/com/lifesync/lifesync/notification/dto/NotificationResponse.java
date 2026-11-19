package com.lifesync.lifesync.notification.dto;
import com.lifesync.lifesync.notification.entity.NotificationType;
import java.time.LocalDateTime;
public class NotificationResponse {
    private Long id; private String title; private String message; private NotificationType notificationType; private Boolean isRead; private Long reminderId; private String reminderTitle; private Long taskId; private String taskTitle; private Long goalId; private String goalTitle; private Long milestoneId; private String milestoneTitle; private Long habitId; private String habitName; private LocalDateTime createdAt;
    public NotificationResponse() { }
    public NotificationResponse(Long id,String title,String message,NotificationType notificationType,Boolean isRead,Long reminderId,String reminderTitle,Long taskId,String taskTitle,Long goalId,String goalTitle,Long milestoneId,String milestoneTitle,Long habitId,String habitName,LocalDateTime createdAt){this.id=id;this.title=title;this.message=message;this.notificationType=notificationType;this.isRead=isRead;this.reminderId=reminderId;this.reminderTitle=reminderTitle;this.taskId=taskId;this.taskTitle=taskTitle;this.goalId=goalId;this.goalTitle=goalTitle;this.milestoneId=milestoneId;this.milestoneTitle=milestoneTitle;this.habitId=habitId;this.habitName=habitName;this.createdAt=createdAt;}
    public Long getId(){return id;} public void setId(Long v){id=v;} public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getMessage(){return message;} public void setMessage(String v){message=v;} public NotificationType getNotificationType(){return notificationType;} public void setNotificationType(NotificationType v){notificationType=v;}
    public Boolean getIsRead(){return isRead;} public void setIsRead(Boolean v){isRead=v;} public Long getReminderId(){return reminderId;} public void setReminderId(Long v){reminderId=v;}
    public String getReminderTitle(){return reminderTitle;} public void setReminderTitle(String v){reminderTitle=v;}
    public Long getTaskId(){return taskId;} public void setTaskId(Long v){taskId=v;} public String getTaskTitle(){return taskTitle;} public void setTaskTitle(String v){taskTitle=v;}
    public Long getGoalId(){return goalId;} public void setGoalId(Long v){goalId=v;} public String getGoalTitle(){return goalTitle;} public void setGoalTitle(String v){goalTitle=v;}
    public Long getMilestoneId(){return milestoneId;} public void setMilestoneId(Long v){milestoneId=v;} public String getMilestoneTitle(){return milestoneTitle;} public void setMilestoneTitle(String v){milestoneTitle=v;}
    public Long getHabitId(){return habitId;} public void setHabitId(Long v){habitId=v;} public String getHabitName(){return habitName;} public void setHabitName(String v){habitName=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
}

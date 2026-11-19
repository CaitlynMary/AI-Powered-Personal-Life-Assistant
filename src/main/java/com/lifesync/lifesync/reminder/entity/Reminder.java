package com.lifesync.lifesync.reminder.entity;

import com.lifesync.lifesync.task.entity.Task;
import com.lifesync.lifesync.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.LocalDateTime;

@Entity
@Table(name="reminders",indexes={
        @Index(name="idx_reminder_user_date",columnList="user_id, reminder_date_time"),
        @Index(name="idx_reminder_status_date",columnList="status, reminder_date_time")})
public class Reminder {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=200) private String title;
    @Column(nullable=false,length=2000) private String message;
    @Column(name="reminder_date_time",nullable=false) private LocalDateTime reminderDateTime;
    @Column(name="deadline_offset_minutes") private Integer deadlineOffsetMinutes;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private ReminderStatus status=ReminderStatus.PENDING;
    @Enumerated(EnumType.STRING) @Column(name="reminder_type",nullable=false,length=30) private ReminderType reminderType;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false,foreignKey=@ForeignKey(name="fk_reminder_user")) private User user;
    @ManyToOne(fetch=FetchType.LAZY) @OnDelete(action=OnDeleteAction.SET_NULL) @JoinColumn(name="task_id",foreignKey=@ForeignKey(name="fk_reminder_task")) private Task task;
    @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
    @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
    public Reminder() { }
    @PrePersist public void onCreate(){LocalDateTime now=LocalDateTime.now();createdAt=now;updatedAt=now;}
    @PreUpdate public void onUpdate(){updatedAt=LocalDateTime.now();}
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getMessage(){return message;} public void setMessage(String v){message=v;}
    public LocalDateTime getReminderDateTime(){return reminderDateTime;} public void setReminderDateTime(LocalDateTime v){reminderDateTime=v;}
    public Integer getDeadlineOffsetMinutes(){return deadlineOffsetMinutes;} public void setDeadlineOffsetMinutes(Integer v){deadlineOffsetMinutes=v;}
    public ReminderStatus getStatus(){return status;} public void setStatus(ReminderStatus v){status=v;}
    public ReminderType getReminderType(){return reminderType;} public void setReminderType(ReminderType v){reminderType=v;}
    public User getUser(){return user;} public void setUser(User v){user=v;}
    public Task getTask(){return task;} public void setTask(Task v){task=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
}

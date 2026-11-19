package com.lifesync.lifesync.notification.entity;

import com.lifesync.lifesync.reminder.entity.Reminder;
import com.lifesync.lifesync.goal.entity.Goal;
import com.lifesync.lifesync.habit.entity.Habit;
import com.lifesync.lifesync.milestone.entity.GoalMilestone;
import com.lifesync.lifesync.task.entity.Task;
import com.lifesync.lifesync.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.LocalDateTime;

@Entity
@Table(name="notifications",indexes=@Index(name="idx_notification_user_created",columnList="user_id, created_at"))
public class Notification {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=200) private String title;
    @Column(nullable=false,length=2000) private String message;
    @Enumerated(EnumType.STRING) @Column(name="notification_type",nullable=false,length=30) private NotificationType notificationType;
    @Column(name="event_key",unique=true,length=200) private String eventKey;
    @Column(name="is_read",nullable=false) private Boolean isRead=false;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false,foreignKey=@ForeignKey(name="fk_notification_user")) private User user;
    @OneToOne(fetch=FetchType.LAZY) @OnDelete(action=OnDeleteAction.SET_NULL) @JoinColumn(name="reminder_id",unique=true,foreignKey=@ForeignKey(name="fk_notification_reminder")) private Reminder reminder;
    @ManyToOne(fetch=FetchType.LAZY) @OnDelete(action=OnDeleteAction.SET_NULL) @JoinColumn(name="task_id",foreignKey=@ForeignKey(name="fk_notification_task")) private Task task;
    @ManyToOne(fetch=FetchType.LAZY) @OnDelete(action=OnDeleteAction.SET_NULL) @JoinColumn(name="goal_id",foreignKey=@ForeignKey(name="fk_notification_goal")) private Goal goal;
    @ManyToOne(fetch=FetchType.LAZY) @OnDelete(action=OnDeleteAction.SET_NULL) @JoinColumn(name="milestone_id",foreignKey=@ForeignKey(name="fk_notification_milestone")) private GoalMilestone milestone;
    @ManyToOne(fetch=FetchType.LAZY) @OnDelete(action=OnDeleteAction.SET_NULL) @JoinColumn(name="habit_id",foreignKey=@ForeignKey(name="fk_notification_habit")) private Habit habit;
    @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
    public Notification() { }
    @PrePersist public void onCreate(){if(createdAt==null)createdAt=LocalDateTime.now();}
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getMessage(){return message;} public void setMessage(String v){message=v;}
    public NotificationType getNotificationType(){return notificationType;} public void setNotificationType(NotificationType v){notificationType=v;}
    public String getEventKey(){return eventKey;} public void setEventKey(String v){eventKey=v;}
    public Boolean getIsRead(){return isRead;} public void setIsRead(Boolean v){isRead=v;}
    public User getUser(){return user;} public void setUser(User v){user=v;}
    public Reminder getReminder(){return reminder;} public void setReminder(Reminder v){reminder=v;}
    public Task getTask(){return task;} public void setTask(Task v){task=v;}
    public Goal getGoal(){return goal;} public void setGoal(Goal v){goal=v;}
    public GoalMilestone getMilestone(){return milestone;} public void setMilestone(GoalMilestone v){milestone=v;}
    public Habit getHabit(){return habit;} public void setHabit(Habit v){habit=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
}

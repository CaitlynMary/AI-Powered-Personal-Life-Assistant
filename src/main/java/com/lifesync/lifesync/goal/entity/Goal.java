package com.lifesync.lifesync.goal.entity;

import com.lifesync.lifesync.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="goals",indexes={@Index(name="idx_goal_user_target",columnList="user_id, target_date")})
public class Goal {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=150) private String title;
    @Column(length=2000) private String description;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false,foreignKey=@ForeignKey(name="fk_goal_user")) private User user;
    @Enumerated(EnumType.STRING) @Column(name="goal_type",nullable=false,length=20) private GoalType goalType;
    @Column(name="start_date",nullable=false) private LocalDate startDate;
    @Column(name="target_date",nullable=false) private LocalDate targetDate;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private GoalStatus status=GoalStatus.NOT_STARTED;
    @Min(0) @Max(100) @Column(name="progress_percentage",nullable=false) private Integer progressPercentage=0;
    @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
    @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
    public Goal() { }
    @PrePersist public void onCreate(){LocalDateTime now=LocalDateTime.now();createdAt=now;updatedAt=now;}
    @PreUpdate public void onUpdate(){updatedAt=LocalDateTime.now();}
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getTitle(){return title;} public void setTitle(String title){this.title=title;}
    public String getDescription(){return description;} public void setDescription(String description){this.description=description;}
    public User getUser(){return user;} public void setUser(User user){this.user=user;}
    public GoalType getGoalType(){return goalType;} public void setGoalType(GoalType goalType){this.goalType=goalType;}
    public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate startDate){this.startDate=startDate;}
    public LocalDate getTargetDate(){return targetDate;} public void setTargetDate(LocalDate targetDate){this.targetDate=targetDate;}
    public GoalStatus getStatus(){return status;} public void setStatus(GoalStatus status){this.status=status;}
    public Integer getProgressPercentage(){return progressPercentage;} public void setProgressPercentage(Integer progressPercentage){this.progressPercentage=progressPercentage;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime createdAt){this.createdAt=createdAt;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime updatedAt){this.updatedAt=updatedAt;}
}

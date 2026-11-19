package com.lifesync.lifesync.milestone.entity;

import com.lifesync.lifesync.goal.entity.Goal;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="goal_milestones",indexes=@Index(name="idx_milestone_goal_target",columnList="goal_id, target_date"))
public class GoalMilestone {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=150) private String title;
    @Column(length=2000) private String description;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="goal_id",nullable=false,foreignKey=@ForeignKey(name="fk_milestone_goal")) private Goal goal;
    @Column(name="target_date",nullable=false) private LocalDate targetDate;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private MilestoneStatus status=MilestoneStatus.NOT_STARTED;
    @Column(name="completed_at") private LocalDateTime completedAt;
    @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
    @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
    public GoalMilestone() { }
    @PrePersist public void onCreate(){LocalDateTime now=LocalDateTime.now();createdAt=now;updatedAt=now;}
    @PreUpdate public void onUpdate(){updatedAt=LocalDateTime.now();}
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getTitle(){return title;} public void setTitle(String title){this.title=title;}
    public String getDescription(){return description;} public void setDescription(String description){this.description=description;}
    public Goal getGoal(){return goal;} public void setGoal(Goal goal){this.goal=goal;}
    public LocalDate getTargetDate(){return targetDate;} public void setTargetDate(LocalDate targetDate){this.targetDate=targetDate;}
    public MilestoneStatus getStatus(){return status;} public void setStatus(MilestoneStatus status){this.status=status;}
    public LocalDateTime getCompletedAt(){return completedAt;} public void setCompletedAt(LocalDateTime completedAt){this.completedAt=completedAt;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime createdAt){this.createdAt=createdAt;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime updatedAt){this.updatedAt=updatedAt;}
}

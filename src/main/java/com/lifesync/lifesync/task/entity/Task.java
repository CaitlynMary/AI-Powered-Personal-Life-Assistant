package com.lifesync.lifesync.task.entity;

import com.lifesync.lifesync.category.entity.Category;
import com.lifesync.lifesync.user.entity.User;
import com.lifesync.lifesync.goal.entity.Goal;
import com.lifesync.lifesync.milestone.entity.GoalMilestone;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_task_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_task_user_status", columnList = "user_id, status"),
        @Index(name = "idx_task_user_due", columnList = "user_id, due_date")})
public class Task {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 150) private String title;
    @Column(length = 2000) private String description;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_task_user"))
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", foreignKey = @ForeignKey(name = "fk_task_category"))
    private Category category;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", foreignKey = @ForeignKey(name = "fk_task_goal"))
    private Goal goal;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_id", foreignKey = @ForeignKey(name = "fk_task_milestone"))
    private GoalMilestone milestone;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TaskPriority priority;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TaskStatus status;
    @Enumerated(EnumType.STRING) @Column(name = "status_before_completion", length = 20) private TaskStatus statusBeforeCompletion;
    @Column(name = "due_date") private LocalDateTime dueDate;
    @Column(name = "start_date") private LocalDateTime startDate;
    @Column(name = "estimated_duration") private Integer estimatedDuration;
    @Column(name = "actual_duration") private Integer actualDuration;
    @Column(name = "completed_at") private LocalDateTime completedAt;
    @Column(name = "missed_at") private LocalDateTime missedAt;
    @Column(nullable = false) private Boolean recurring = false;
    @Enumerated(EnumType.STRING) @Column(name = "recurrence_type", nullable = false, length = 20) private RecurrenceType recurrenceType = RecurrenceType.NONE;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    public Task() { }
    @PrePersist public void onCreate() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate public void onUpdate() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Goal getGoal() { return goal; }
    public void setGoal(Goal goal) { this.goal = goal; }
    public GoalMilestone getMilestone() { return milestone; }
    public void setMilestone(GoalMilestone milestone) { this.milestone = milestone; }
    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public TaskStatus getStatusBeforeCompletion() { return statusBeforeCompletion; }
    public void setStatusBeforeCompletion(TaskStatus statusBeforeCompletion) { this.statusBeforeCompletion = statusBeforeCompletion; }
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public Integer getEstimatedDuration() { return estimatedDuration; }
    public void setEstimatedDuration(Integer estimatedDuration) { this.estimatedDuration = estimatedDuration; }
    public Integer getActualDuration() { return actualDuration; }
    public void setActualDuration(Integer actualDuration) { this.actualDuration = actualDuration; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getMissedAt() { return missedAt; }
    public void setMissedAt(LocalDateTime missedAt) { this.missedAt = missedAt; }
    public Boolean getRecurring() { return recurring; }
    public void setRecurring(Boolean recurring) { this.recurring = recurring; }
    public RecurrenceType getRecurrenceType() { return recurrenceType; }
    public void setRecurrenceType(RecurrenceType recurrenceType) { this.recurrenceType = recurrenceType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

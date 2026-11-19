package com.lifesync.lifesync.task.dto;
import com.lifesync.lifesync.task.entity.RecurrenceType;
import com.lifesync.lifesync.task.entity.TaskPriority;
import com.lifesync.lifesync.task.entity.TaskLifecycleStatus;
import java.time.LocalDateTime;
public class TaskResponse {
    private Long id; private String title; private String description; private Long categoryId; private String categoryName; private Long goalId; private Long milestoneId;
    private TaskPriority priority; private TaskLifecycleStatus status; private LocalDateTime dueDate; private LocalDateTime startDate;
    private Integer estimatedDuration; private Integer actualDuration; private LocalDateTime completedAt; private LocalDateTime missedAt; private Boolean recurring;
    private RecurrenceType recurrenceType; private LocalDateTime createdAt; private LocalDateTime updatedAt;
    public TaskResponse() { }
    public TaskResponse(Long id,String title,String description,Long categoryId,String categoryName,TaskPriority priority,TaskLifecycleStatus status,LocalDateTime dueDate,LocalDateTime startDate,Integer estimatedDuration,Integer actualDuration,LocalDateTime completedAt,LocalDateTime missedAt,Boolean recurring,RecurrenceType recurrenceType,LocalDateTime createdAt,LocalDateTime updatedAt) {
        this.id=id;this.title=title;this.description=description;this.categoryId=categoryId;this.categoryName=categoryName;this.priority=priority;this.status=status;this.dueDate=dueDate;this.startDate=startDate;this.estimatedDuration=estimatedDuration;this.actualDuration=actualDuration;this.completedAt=completedAt;this.missedAt=missedAt;this.recurring=recurring;this.recurrenceType=recurrenceType;this.createdAt=createdAt;this.updatedAt=updatedAt;
    }
    public TaskResponse(Long id,String title,String description,Long categoryId,String categoryName,Long goalId,Long milestoneId,TaskPriority priority,TaskLifecycleStatus status,LocalDateTime dueDate,LocalDateTime startDate,Integer estimatedDuration,Integer actualDuration,LocalDateTime completedAt,LocalDateTime missedAt,Boolean recurring,RecurrenceType recurrenceType,LocalDateTime createdAt,LocalDateTime updatedAt) {
        this(id,title,description,categoryId,categoryName,priority,status,dueDate,startDate,estimatedDuration,actualDuration,completedAt,missedAt,recurring,recurrenceType,createdAt,updatedAt);this.goalId=goalId;this.milestoneId=milestoneId;
    }
    public Long getId(){return id;} public void setId(Long id){this.id=id;} public String getTitle(){return title;} public void setTitle(String title){this.title=title;}
    public String getDescription(){return description;} public void setDescription(String description){this.description=description;} public Long getCategoryId(){return categoryId;} public void setCategoryId(Long categoryId){this.categoryId=categoryId;}
    public String getCategoryName(){return categoryName;} public void setCategoryName(String categoryName){this.categoryName=categoryName;} public TaskPriority getPriority(){return priority;} public void setPriority(TaskPriority priority){this.priority=priority;}
    public Long getGoalId(){return goalId;} public void setGoalId(Long goalId){this.goalId=goalId;} public Long getMilestoneId(){return milestoneId;} public void setMilestoneId(Long milestoneId){this.milestoneId=milestoneId;}
    public TaskLifecycleStatus getStatus(){return status;} public void setStatus(TaskLifecycleStatus status){this.status=status;} public LocalDateTime getDueDate(){return dueDate;} public void setDueDate(LocalDateTime dueDate){this.dueDate=dueDate;}
    public LocalDateTime getStartDate(){return startDate;} public void setStartDate(LocalDateTime startDate){this.startDate=startDate;} public Integer getEstimatedDuration(){return estimatedDuration;} public void setEstimatedDuration(Integer estimatedDuration){this.estimatedDuration=estimatedDuration;}
    public Integer getActualDuration(){return actualDuration;} public void setActualDuration(Integer actualDuration){this.actualDuration=actualDuration;} public LocalDateTime getCompletedAt(){return completedAt;} public void setCompletedAt(LocalDateTime completedAt){this.completedAt=completedAt;} public LocalDateTime getMissedAt(){return missedAt;} public void setMissedAt(LocalDateTime missedAt){this.missedAt=missedAt;}
    public Boolean getRecurring(){return recurring;} public void setRecurring(Boolean recurring){this.recurring=recurring;} public RecurrenceType getRecurrenceType(){return recurrenceType;} public void setRecurrenceType(RecurrenceType recurrenceType){this.recurrenceType=recurrenceType;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime createdAt){this.createdAt=createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime updatedAt){this.updatedAt=updatedAt;}
}

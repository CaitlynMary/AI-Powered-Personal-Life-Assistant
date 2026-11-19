package com.lifesync.lifesync.task.dto;
import com.lifesync.lifesync.task.entity.RecurrenceType;
import com.lifesync.lifesync.task.entity.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
public class UpdateTaskRequest {
    @NotBlank @Size(max=150) private String title;
    @Size(max=2000) private String description;
    @Positive private Long categoryId;
    @Positive private Long goalId;
    @Positive private Long milestoneId;
    @NotNull private TaskPriority priority;
    private LocalDateTime dueDate;
    private LocalDateTime startDate;
    @Positive private Integer estimatedDuration;
    @NotNull private Boolean recurring;
    @NotNull private RecurrenceType recurrenceType;
    public UpdateTaskRequest() { }
    public UpdateTaskRequest(String title,String description,Long categoryId,TaskPriority priority,LocalDateTime dueDate,LocalDateTime startDate,Integer estimatedDuration,Boolean recurring,RecurrenceType recurrenceType) {
        this.title=title;this.description=description;this.categoryId=categoryId;this.priority=priority;this.dueDate=dueDate;this.startDate=startDate;this.estimatedDuration=estimatedDuration;this.recurring=recurring;this.recurrenceType=recurrenceType;
    }
    public UpdateTaskRequest(String title,String description,Long categoryId,Long goalId,Long milestoneId,TaskPriority priority,LocalDateTime dueDate,LocalDateTime startDate,Integer estimatedDuration,Boolean recurring,RecurrenceType recurrenceType) {
        this(title,description,categoryId,priority,dueDate,startDate,estimatedDuration,recurring,recurrenceType);this.goalId=goalId;this.milestoneId=milestoneId;
    }
    public String getTitle(){return title;} public void setTitle(String title){this.title=title;}
    public String getDescription(){return description;} public void setDescription(String description){this.description=description;}
    public Long getCategoryId(){return categoryId;} public void setCategoryId(Long categoryId){this.categoryId=categoryId;}
    public Long getGoalId(){return goalId;} public void setGoalId(Long goalId){this.goalId=goalId;}
    public Long getMilestoneId(){return milestoneId;} public void setMilestoneId(Long milestoneId){this.milestoneId=milestoneId;}
    public TaskPriority getPriority(){return priority;} public void setPriority(TaskPriority priority){this.priority=priority;}
    public LocalDateTime getDueDate(){return dueDate;} public void setDueDate(LocalDateTime dueDate){this.dueDate=dueDate;}
    public LocalDateTime getStartDate(){return startDate;} public void setStartDate(LocalDateTime startDate){this.startDate=startDate;}
    public Integer getEstimatedDuration(){return estimatedDuration;} public void setEstimatedDuration(Integer estimatedDuration){this.estimatedDuration=estimatedDuration;}
    public Boolean getRecurring(){return recurring;} public void setRecurring(Boolean recurring){this.recurring=recurring;}
    public RecurrenceType getRecurrenceType(){return recurrenceType;} public void setRecurrenceType(RecurrenceType recurrenceType){this.recurrenceType=recurrenceType;}
}

package com.lifesync.lifesync.goal.dto;
import com.lifesync.lifesync.goal.entity.GoalLifecycleStatus;
import com.lifesync.lifesync.goal.entity.GoalType;
import java.time.LocalDate;
import java.time.LocalDateTime;
public class GoalResponse {
    private Long id; private String title; private String description; private GoalType goalType; private LocalDate startDate; private LocalDate targetDate;
    private GoalLifecycleStatus status; private Integer progressPercentage; private LocalDateTime createdAt; private LocalDateTime updatedAt;
    public GoalResponse() { }
    public GoalResponse(Long id,String title,String description,GoalType goalType,LocalDate startDate,LocalDate targetDate,GoalLifecycleStatus status,Integer progressPercentage,LocalDateTime createdAt,LocalDateTime updatedAt){this.id=id;this.title=title;this.description=description;this.goalType=goalType;this.startDate=startDate;this.targetDate=targetDate;this.status=status;this.progressPercentage=progressPercentage;this.createdAt=createdAt;this.updatedAt=updatedAt;}
    public Long getId(){return id;} public void setId(Long v){id=v;} public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;} public GoalType getGoalType(){return goalType;} public void setGoalType(GoalType v){goalType=v;}
    public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;} public LocalDate getTargetDate(){return targetDate;} public void setTargetDate(LocalDate v){targetDate=v;}
    public GoalLifecycleStatus getStatus(){return status;} public void setStatus(GoalLifecycleStatus v){status=v;} public Integer getProgressPercentage(){return progressPercentage;} public void setProgressPercentage(Integer v){progressPercentage=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
}

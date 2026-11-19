package com.lifesync.lifesync.milestone.dto;
import com.lifesync.lifesync.milestone.entity.MilestoneLifecycleStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
public class MilestoneResponse {
    private Long id; private Long goalId; private String title; private String description; private LocalDate targetDate; private MilestoneLifecycleStatus status;
    private LocalDateTime completedAt; private LocalDateTime createdAt; private LocalDateTime updatedAt;
    public MilestoneResponse() { }
    public MilestoneResponse(Long id,Long goalId,String title,String description,LocalDate targetDate,MilestoneLifecycleStatus status,LocalDateTime completedAt,LocalDateTime createdAt,LocalDateTime updatedAt){this.id=id;this.goalId=goalId;this.title=title;this.description=description;this.targetDate=targetDate;this.status=status;this.completedAt=completedAt;this.createdAt=createdAt;this.updatedAt=updatedAt;}
    public Long getId(){return id;} public void setId(Long v){id=v;} public Long getGoalId(){return goalId;} public void setGoalId(Long v){goalId=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public LocalDate getTargetDate(){return targetDate;} public void setTargetDate(LocalDate v){targetDate=v;} public MilestoneLifecycleStatus getStatus(){return status;} public void setStatus(MilestoneLifecycleStatus v){status=v;}
    public LocalDateTime getCompletedAt(){return completedAt;} public void setCompletedAt(LocalDateTime v){completedAt=v;} public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
}

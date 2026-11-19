package com.lifesync.lifesync.goal.dto;
import com.lifesync.lifesync.goal.entity.GoalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
public class GoalRequest {
    @NotBlank @Size(max=150) private String title;
    @Size(max=2000) private String description;
    @NotNull private GoalType goalType;
    @NotNull private LocalDate startDate;
    @NotNull private LocalDate targetDate;
    public GoalRequest() { }
    public GoalRequest(String title,String description,GoalType goalType,LocalDate startDate,LocalDate targetDate){this.title=title;this.description=description;this.goalType=goalType;this.startDate=startDate;this.targetDate=targetDate;}
    public String getTitle(){return title;} public void setTitle(String value){title=value;}
    public String getDescription(){return description;} public void setDescription(String value){description=value;}
    public GoalType getGoalType(){return goalType;} public void setGoalType(GoalType value){goalType=value;}
    public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate value){startDate=value;}
    public LocalDate getTargetDate(){return targetDate;} public void setTargetDate(LocalDate value){targetDate=value;}
}

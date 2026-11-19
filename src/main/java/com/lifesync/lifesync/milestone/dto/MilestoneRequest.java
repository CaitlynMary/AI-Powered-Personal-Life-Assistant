package com.lifesync.lifesync.milestone.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
public class MilestoneRequest {
    @NotBlank @Size(max=150) private String title;
    @Size(max=2000) private String description;
    @NotNull private LocalDate targetDate;
    public MilestoneRequest() { }
    public MilestoneRequest(String title,String description,LocalDate targetDate){this.title=title;this.description=description;this.targetDate=targetDate;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public LocalDate getTargetDate(){return targetDate;} public void setTargetDate(LocalDate v){targetDate=v;}
}

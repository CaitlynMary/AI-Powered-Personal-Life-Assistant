package com.lifesync.lifesync.habit.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
public class HabitCompletionRequest {
    @NotNull private LocalDate completionDate;
    @Size(max=1000) private String notes;
    private Boolean completed;
    public HabitCompletionRequest() { }
    public HabitCompletionRequest(LocalDate completionDate,String notes){this(completionDate,notes,null);}
    public HabitCompletionRequest(LocalDate completionDate,String notes,Boolean completed){this.completionDate=completionDate;this.notes=notes;this.completed=completed;}
    public LocalDate getCompletionDate(){return completionDate;} public void setCompletionDate(LocalDate v){completionDate=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public Boolean getCompleted(){return completed;} public void setCompleted(Boolean v){completed=v;}
}

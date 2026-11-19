package com.lifesync.lifesync.habit.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
public class HabitLogRequest {
    @NotNull private LocalDate completionDate;
    @NotNull private Boolean completed;
    @Size(max=1000) private String notes;
    public HabitLogRequest() { }
    public HabitLogRequest(LocalDate completionDate,Boolean completed,String notes){this.completionDate=completionDate;this.completed=completed;this.notes=notes;}
    public LocalDate getCompletionDate(){return completionDate;} public void setCompletionDate(LocalDate v){completionDate=v;}
    public Boolean getCompleted(){return completed;} public void setCompleted(Boolean v){completed=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
}

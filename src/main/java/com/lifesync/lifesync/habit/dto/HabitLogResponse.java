package com.lifesync.lifesync.habit.dto;
import java.time.LocalDate;
import java.time.LocalDateTime;
public class HabitLogResponse {
    private Long id; private Long habitId; private LocalDate completionDate; private Boolean completed; private String notes; private LocalDateTime createdAt;
    public HabitLogResponse() { }
    public HabitLogResponse(Long id,Long habitId,LocalDate completionDate,Boolean completed,String notes,LocalDateTime createdAt){this.id=id;this.habitId=habitId;this.completionDate=completionDate;this.completed=completed;this.notes=notes;this.createdAt=createdAt;}
    public Long getId(){return id;} public void setId(Long v){id=v;} public Long getHabitId(){return habitId;} public void setHabitId(Long v){habitId=v;}
    public LocalDate getCompletionDate(){return completionDate;} public void setCompletionDate(LocalDate v){completionDate=v;} public Boolean getCompleted(){return completed;} public void setCompleted(Boolean v){completed=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;} public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
}

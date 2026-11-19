package com.lifesync.lifesync.habit.dto;
import java.time.LocalDate;
public class HabitCalendarDay {
    private LocalDate date; private boolean eligible; private boolean completed; private String notes;
    public HabitCalendarDay() { }
    public HabitCalendarDay(LocalDate date,boolean eligible,boolean completed,String notes){this.date=date;this.eligible=eligible;this.completed=completed;this.notes=notes;}
    public LocalDate getDate(){return date;} public void setDate(LocalDate v){date=v;} public boolean isEligible(){return eligible;} public void setEligible(boolean v){eligible=v;}
    public boolean isCompleted(){return completed;} public void setCompleted(boolean v){completed=v;} public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
}

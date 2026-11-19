package com.lifesync.lifesync.habit.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="habit_logs",uniqueConstraints=@UniqueConstraint(name="uk_habit_log_date",columnNames={"habit_id","completion_date"}))
public class HabitLog {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="habit_id",nullable=false,foreignKey=@ForeignKey(name="fk_habit_log_habit")) private Habit habit;
    @Column(name="completion_date",nullable=false) private LocalDate completionDate;
    @Column(nullable=false) private Boolean completed;
    @Column(length=1000) private String notes;
    @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
    public HabitLog() { }
    @PrePersist public void onCreate(){if(createdAt==null)createdAt=LocalDateTime.now();}
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Habit getHabit(){return habit;} public void setHabit(Habit v){habit=v;}
    public LocalDate getCompletionDate(){return completionDate;} public void setCompletionDate(LocalDate v){completionDate=v;}
    public Boolean getCompleted(){return completed;} public void setCompleted(Boolean v){completed=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
}

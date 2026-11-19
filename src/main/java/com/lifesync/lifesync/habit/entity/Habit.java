package com.lifesync.lifesync.habit.entity;

import com.lifesync.lifesync.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="habits",indexes=@Index(name="idx_habit_user_active",columnList="user_id, active"))
public class Habit {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=150) private String name;
    @Column(length=2000) private String description;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false,foreignKey=@ForeignKey(name="fk_habit_user")) private User user;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private HabitFrequency frequency;
    @Min(1) @Max(7) @Column(name="target_days_per_week",nullable=false) private Integer targetDaysPerWeek;
    @Column(name="start_date",nullable=false) private LocalDate startDate;
    @Column(name="end_date") private LocalDate endDate;
    @Column(nullable=false) private Boolean active=true;
    @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
    @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
    public Habit() { }
    @PrePersist public void onCreate(){LocalDateTime now=LocalDateTime.now();createdAt=now;updatedAt=now;}
    @PreUpdate public void onUpdate(){updatedAt=LocalDateTime.now();}
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public User getUser(){return user;} public void setUser(User v){user=v;}
    public HabitFrequency getFrequency(){return frequency;} public void setFrequency(HabitFrequency v){frequency=v;}
    public Integer getTargetDaysPerWeek(){return targetDaysPerWeek;} public void setTargetDaysPerWeek(Integer v){targetDaysPerWeek=v;}
    public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;}
    public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;}
    public Boolean getActive(){return active;} public void setActive(Boolean v){active=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
}

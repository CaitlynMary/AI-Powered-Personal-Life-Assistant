package com.lifesync.lifesync.habit.dto;
import com.lifesync.lifesync.habit.entity.HabitFrequency;
import java.time.LocalDate;
import java.time.LocalDateTime;
public class HabitResponse {
    private Long id; private String name; private String description; private HabitFrequency frequency; private Integer targetDaysPerWeek;
    private LocalDate startDate; private LocalDate endDate; private Boolean active; private LocalDateTime createdAt; private LocalDateTime updatedAt;
    public HabitResponse() { }
    public HabitResponse(Long id,String name,String description,HabitFrequency frequency,Integer targetDaysPerWeek,LocalDate startDate,LocalDate endDate,Boolean active,LocalDateTime createdAt,LocalDateTime updatedAt){this.id=id;this.name=name;this.description=description;this.frequency=frequency;this.targetDaysPerWeek=targetDaysPerWeek;this.startDate=startDate;this.endDate=endDate;this.active=active;this.createdAt=createdAt;this.updatedAt=updatedAt;}
    public Long getId(){return id;} public void setId(Long v){id=v;} public String getName(){return name;} public void setName(String v){name=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;} public HabitFrequency getFrequency(){return frequency;} public void setFrequency(HabitFrequency v){frequency=v;}
    public Integer getTargetDaysPerWeek(){return targetDaysPerWeek;} public void setTargetDaysPerWeek(Integer v){targetDaysPerWeek=v;}
    public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;} public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;}
    public Boolean getActive(){return active;} public void setActive(Boolean v){active=v;} public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
}

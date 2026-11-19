package com.lifesync.lifesync.habit.dto;
import com.lifesync.lifesync.habit.entity.HabitFrequency;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
public class HabitRequest {
    @NotBlank @Size(max=150) private String name;
    @Size(max=2000) private String description;
    @NotNull private HabitFrequency frequency;
    @NotNull @Min(1) @Max(7) private Integer targetDaysPerWeek;
    @NotNull private LocalDate startDate;
    private LocalDate endDate;
    public HabitRequest() { }
    public HabitRequest(String name,String description,HabitFrequency frequency,Integer targetDaysPerWeek,LocalDate startDate,LocalDate endDate){this.name=name;this.description=description;this.frequency=frequency;this.targetDaysPerWeek=targetDaysPerWeek;this.startDate=startDate;this.endDate=endDate;}
    public String getName(){return name;} public void setName(String v){name=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public HabitFrequency getFrequency(){return frequency;} public void setFrequency(HabitFrequency v){frequency=v;} public Integer getTargetDaysPerWeek(){return targetDaysPerWeek;} public void setTargetDaysPerWeek(Integer v){targetDaysPerWeek=v;}
    public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;} public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;}
}

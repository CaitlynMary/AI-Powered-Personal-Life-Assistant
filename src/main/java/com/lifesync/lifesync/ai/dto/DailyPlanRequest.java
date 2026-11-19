package com.lifesync.lifesync.ai.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public class DailyPlanRequest {
    @NotNull(message = "date is required")
    private LocalDate date;
    private LocalTime dayStartTime = LocalTime.of(8, 0);
    private LocalTime dayEndTime = LocalTime.of(20, 0);
    public DailyPlanRequest() { }
    public DailyPlanRequest(LocalDate date) { this.date=date; }
    public DailyPlanRequest(LocalDate date, LocalTime dayStartTime, LocalTime dayEndTime) {
        this.date=date; this.dayStartTime=dayStartTime; this.dayEndTime=dayEndTime;
    }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date=date; }
    public LocalTime getDayStartTime() { return dayStartTime; }
    public void setDayStartTime(LocalTime dayStartTime) { this.dayStartTime=dayStartTime; }
    public LocalTime getDayEndTime() { return dayEndTime; }
    public void setDayEndTime(LocalTime dayEndTime) { this.dayEndTime=dayEndTime; }
}

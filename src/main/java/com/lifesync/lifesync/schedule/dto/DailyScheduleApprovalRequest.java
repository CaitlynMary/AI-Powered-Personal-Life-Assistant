package com.lifesync.lifesync.schedule.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public class DailyScheduleApprovalRequest {
    @NotNull
    private LocalDate date;

    @NotEmpty
    @Size(max = 30)
    @Valid
    private List<DailyScheduleApprovalItem> items;

    public DailyScheduleApprovalRequest() { }

    public DailyScheduleApprovalRequest(LocalDate date, List<DailyScheduleApprovalItem> items) {
        this.date = date;
        this.items = items;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public List<DailyScheduleApprovalItem> getItems() { return items; }
    public void setItems(List<DailyScheduleApprovalItem> items) { this.items = items; }
}

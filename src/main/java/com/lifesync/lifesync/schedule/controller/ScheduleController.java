package com.lifesync.lifesync.schedule.controller;

import com.lifesync.lifesync.schedule.dto.ScheduleConflictRequest;
import com.lifesync.lifesync.schedule.dto.ScheduleConflictResponse;
import com.lifesync.lifesync.schedule.dto.DailyScheduleApprovalRequest;
import com.lifesync.lifesync.schedule.dto.SchedulePageResponse;
import com.lifesync.lifesync.schedule.dto.ScheduleSlotRequest;
import com.lifesync.lifesync.schedule.dto.ScheduleSlotResponse;
import com.lifesync.lifesync.schedule.service.ScheduleService;
import com.lifesync.lifesync.security.principal.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {
    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) { this.scheduleService = scheduleService; }

    @PostMapping
    public ResponseEntity<ScheduleSlotResponse> create(@AuthenticationPrincipal CustomUserDetails principal,
                                                        @Valid @RequestBody ScheduleSlotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.create(principal.getId(), request));
    }

    @PostMapping("/approve-daily-plan")
    public List<ScheduleSlotResponse> approveDailyPlan(@AuthenticationPrincipal CustomUserDetails principal,
                                                        @Valid @RequestBody DailyScheduleApprovalRequest request) {
        return scheduleService.approveDailyPlan(principal.getId(), request);
    }

    @GetMapping
    public SchedulePageResponse getAll(@AuthenticationPrincipal CustomUserDetails principal,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return scheduleService.getAll(principal.getId(), page, size);
    }

    @GetMapping("/daily")
    public List<ScheduleSlotResponse> daily(@AuthenticationPrincipal CustomUserDetails principal,
                                            @RequestParam LocalDate date) {
        return scheduleService.daily(principal.getId(), date);
    }

    @GetMapping("/weekly")
    public List<ScheduleSlotResponse> weekly(@AuthenticationPrincipal CustomUserDetails principal,
                                             @RequestParam LocalDate startDate) {
        return scheduleService.weekly(principal.getId(), startDate);
    }

    @PostMapping("/conflicts")
    public ScheduleConflictResponse conflicts(@AuthenticationPrincipal CustomUserDetails principal,
                                               @Valid @RequestBody ScheduleConflictRequest request) {
        return scheduleService.detectConflicts(principal.getId(), request);
    }

    @GetMapping("/{id}")
    public ScheduleSlotResponse getById(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        return scheduleService.getById(principal.getId(), id);
    }

    @PutMapping("/{id}")
    public ScheduleSlotResponse update(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id,
                                       @Valid @RequestBody ScheduleSlotRequest request) {
        return scheduleService.update(principal.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        scheduleService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}

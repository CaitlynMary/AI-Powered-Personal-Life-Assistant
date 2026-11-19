package com.lifesync.lifesync.schedule.service;

import com.lifesync.lifesync.schedule.dto.*;
import java.time.LocalDate;
import java.util.List;

public interface ScheduleService {
    ScheduleSlotResponse create(Long userId, ScheduleSlotRequest request);
    SchedulePageResponse getAll(Long userId, int page, int size);
    ScheduleSlotResponse getById(Long userId, Long id);
    ScheduleSlotResponse update(Long userId, Long id, ScheduleSlotRequest request);
    void delete(Long userId, Long id);
    List<ScheduleSlotResponse> daily(Long userId, LocalDate date);
    List<ScheduleSlotResponse> weekly(Long userId, LocalDate startDate);
    ScheduleConflictResponse detectConflicts(Long userId, ScheduleConflictRequest request);
    List<ScheduleSlotResponse> approveDailyPlan(Long userId, DailyScheduleApprovalRequest request);
}

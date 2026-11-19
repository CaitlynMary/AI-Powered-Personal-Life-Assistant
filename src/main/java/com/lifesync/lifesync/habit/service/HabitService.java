package com.lifesync.lifesync.habit.service;
import com.lifesync.lifesync.habit.dto.*;
import java.util.List;
public interface HabitService {
    HabitResponse create(Long userId,HabitRequest request);
    List<HabitResponse> getAll(Long userId);
    HabitResponse getById(Long userId,Long habitId);
    HabitResponse update(Long userId,Long habitId,HabitRequest request);
    void delete(Long userId,Long habitId);
    HabitResponse deactivate(Long userId,Long habitId);
    HabitLogResponse createLog(Long userId,Long habitId,HabitLogRequest request);
    List<HabitLogResponse> getLogs(Long userId,Long habitId);
    HabitLogResponse complete(Long userId,Long habitId,HabitCompletionRequest request);
    HabitStreakResponse streak(Long userId,Long habitId);
    HabitStatisticsResponse statistics(Long userId);
    HabitCalendarResponse calendar(Long userId,Long habitId,int year,int month);
}

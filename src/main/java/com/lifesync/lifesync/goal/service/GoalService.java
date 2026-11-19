package com.lifesync.lifesync.goal.service;
import com.lifesync.lifesync.goal.dto.*;
import com.lifesync.lifesync.goal.entity.GoalStatus;
import java.util.List;
public interface GoalService {
    GoalResponse create(Long userId,GoalRequest request);
    List<GoalResponse> getAll(Long userId);
    GoalResponse getById(Long userId,Long id);
    GoalResponse update(Long userId,Long id,GoalRequest request);
    GoalResponse updateStatus(Long userId,Long id,GoalStatus status);
    void delete(Long userId,Long id);
    GoalStatisticsResponse statistics(Long userId);
}

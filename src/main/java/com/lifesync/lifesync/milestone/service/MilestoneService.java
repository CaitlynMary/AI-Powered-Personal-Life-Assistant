package com.lifesync.lifesync.milestone.service;
import com.lifesync.lifesync.milestone.dto.MilestoneRequest;
import com.lifesync.lifesync.milestone.dto.MilestoneResponse;
import com.lifesync.lifesync.milestone.entity.MilestoneStatus;
import java.util.List;
public interface MilestoneService {
    MilestoneResponse create(Long userId,Long goalId,MilestoneRequest request);
    List<MilestoneResponse> getForGoal(Long userId,Long goalId);
    MilestoneResponse getById(Long userId,Long id);
    MilestoneResponse update(Long userId,Long id,MilestoneRequest request);
    MilestoneResponse updateStatus(Long userId,Long id,MilestoneStatus status);
    MilestoneResponse complete(Long userId,Long id);
    void synchronizeTaskProgress(Long userId,Long milestoneId);
    void delete(Long userId,Long id);
}

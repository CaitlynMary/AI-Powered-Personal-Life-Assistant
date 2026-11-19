package com.lifesync.lifesync.milestone.entity;

import java.time.LocalDate;

public enum MilestoneLifecycleStatus {
    NOT_STARTED, IN_PROGRESS, COMPLETED, OVERDUE, CANCELLED;

    public static MilestoneLifecycleStatus of(GoalMilestone milestone, LocalDate today) {
        MilestoneStatus status = milestone.getStatus();
        if (milestone.getTargetDate() != null
                && milestone.getTargetDate().isBefore(today)
                && (status == MilestoneStatus.NOT_STARTED || status == MilestoneStatus.IN_PROGRESS)) {
            return OVERDUE;
        }
        return valueOf(status.name());
    }
}

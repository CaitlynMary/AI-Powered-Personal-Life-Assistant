package com.lifesync.lifesync.goal.entity;

import java.time.LocalDate;

public enum GoalLifecycleStatus {
    NOT_STARTED, IN_PROGRESS, COMPLETED, OVERDUE, CANCELLED;

    public static GoalLifecycleStatus of(Goal goal, LocalDate today) {
        GoalStatus status = goal.getStatus();
        if (goal.getTargetDate() != null
                && goal.getTargetDate().isBefore(today)
                && (status == GoalStatus.NOT_STARTED || status == GoalStatus.IN_PROGRESS)) {
            return OVERDUE;
        }
        return valueOf(status.name());
    }
}

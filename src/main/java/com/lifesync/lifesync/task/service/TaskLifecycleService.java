package com.lifesync.lifesync.task.service;

import com.lifesync.lifesync.task.entity.Task;
import com.lifesync.lifesync.task.entity.TaskLifecycleStatus;
import com.lifesync.lifesync.task.entity.TaskStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TaskLifecycleService {
    public TaskLifecycleStatus statusOf(Task task, LocalDateTime now) {
        TaskStatus status = task.getStatus();
        if (task.getDueDate() != null
                && task.getDueDate().isBefore(now)
                && (status == TaskStatus.TODO || status == TaskStatus.IN_PROGRESS)) {
            return TaskLifecycleStatus.OVERDUE;
        }
        return TaskLifecycleStatus.valueOf(status.name());
    }

    public boolean isOverdue(Task task, LocalDateTime now) {
        return statusOf(task, now) == TaskLifecycleStatus.OVERDUE;
    }
}

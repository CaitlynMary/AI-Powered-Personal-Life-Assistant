package com.lifesync.lifesync.task.service;

import com.lifesync.lifesync.task.entity.Task;
import com.lifesync.lifesync.task.entity.TaskLifecycleStatus;
import com.lifesync.lifesync.task.entity.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskLifecycleServiceTest {
    private final TaskLifecycleService lifecycle = new TaskLifecycleService();

    @Test void overdueRequiresAnActiveTaskWithAPastDeadline() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 15, 10, 0);
        Task task = new Task();
        task.setStatus(TaskStatus.TODO);

        assertEquals(TaskLifecycleStatus.TODO, lifecycle.statusOf(task, now));
        task.setDueDate(now);
        assertEquals(TaskLifecycleStatus.TODO, lifecycle.statusOf(task, now));
        task.setDueDate(now.minusNanos(1));
        assertEquals(TaskLifecycleStatus.OVERDUE, lifecycle.statusOf(task, now));
        task.setStatus(TaskStatus.IN_PROGRESS);
        assertEquals(TaskLifecycleStatus.OVERDUE, lifecycle.statusOf(task, now));
        task.setStatus(TaskStatus.COMPLETED);
        assertEquals(TaskLifecycleStatus.COMPLETED, lifecycle.statusOf(task, now));
        task.setStatus(TaskStatus.CANCELLED);
        assertEquals(TaskLifecycleStatus.CANCELLED, lifecycle.statusOf(task, now));
        task.setStatus(TaskStatus.MISSED);
        assertEquals(TaskLifecycleStatus.MISSED, lifecycle.statusOf(task, now));
    }
}

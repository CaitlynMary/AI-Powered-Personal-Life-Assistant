package com.lifesync.lifesync.task.service;
import com.lifesync.lifesync.task.dto.*;
import com.lifesync.lifesync.task.entity.TaskPriority;
import com.lifesync.lifesync.task.entity.TaskLifecycleStatus;
import com.lifesync.lifesync.task.entity.TaskStatus;
import java.time.LocalDateTime;
public interface TaskService {
    TaskResponse create(Long userId, TaskRequest request);
    TaskPageResponse getAll(Long userId,int page,int size,String sortBy,String direction,TaskLifecycleStatus status,TaskPriority priority,Long categoryId,LocalDateTime dueFrom,LocalDateTime dueTo,String search);
    TaskResponse getById(Long userId,Long id);
    TaskResponse update(Long userId,Long id,UpdateTaskRequest request);
    TaskResponse updateStatus(Long userId,Long id,TaskStatus status);
    TaskResponse complete(Long userId,Long id);
    void delete(Long userId,Long id);
    TaskStatisticsResponse statistics(Long userId);
}

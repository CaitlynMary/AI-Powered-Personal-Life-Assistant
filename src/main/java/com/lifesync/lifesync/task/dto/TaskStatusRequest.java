package com.lifesync.lifesync.task.dto;
import com.lifesync.lifesync.task.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;
public class TaskStatusRequest {
    @NotNull private TaskStatus status;
    public TaskStatusRequest() { }
    public TaskStatusRequest(TaskStatus status) { this.status=status; }
    public TaskStatus getStatus(){return status;} public void setStatus(TaskStatus status){this.status=status;}
}

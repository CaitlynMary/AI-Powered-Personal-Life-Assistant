package com.lifesync.lifesync.task.controller;
import com.lifesync.lifesync.security.principal.CustomUserDetails;
import com.lifesync.lifesync.task.dto.*;
import com.lifesync.lifesync.task.entity.TaskPriority;
import com.lifesync.lifesync.task.entity.TaskLifecycleStatus;
import com.lifesync.lifesync.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService taskService;
    public TaskController(TaskService taskService) { this.taskService=taskService; }
    @PostMapping public ResponseEntity<TaskResponse> create(@AuthenticationPrincipal CustomUserDetails principal,@Valid @RequestBody TaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(principal.getId(),request));
    }
    @GetMapping public TaskPageResponse getAll(@AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size,
            @RequestParam(defaultValue="createdAt") String sortBy,@RequestParam(defaultValue="DESC") String direction,
            @RequestParam(required=false) TaskLifecycleStatus status,@RequestParam(required=false) TaskPriority priority,
            @RequestParam(required=false) Long categoryId,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueFrom,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueTo,
            @RequestParam(required=false) String search) {
        return taskService.getAll(principal.getId(),page,size,sortBy,direction,status,priority,categoryId,dueFrom,dueTo,search);
    }
    @GetMapping("/statistics") public TaskStatisticsResponse statistics(@AuthenticationPrincipal CustomUserDetails principal) { return taskService.statistics(principal.getId()); }
    @GetMapping("/{id}") public TaskResponse getById(@AuthenticationPrincipal CustomUserDetails principal,@PathVariable Long id) { return taskService.getById(principal.getId(),id); }
    @PutMapping("/{id}") public TaskResponse update(@AuthenticationPrincipal CustomUserDetails principal,@PathVariable Long id,@Valid @RequestBody UpdateTaskRequest request) {
        return taskService.update(principal.getId(),id,request);
    }
    @PatchMapping("/{id}/status") public TaskResponse updateStatus(@AuthenticationPrincipal CustomUserDetails principal,@PathVariable Long id,@Valid @RequestBody TaskStatusRequest request) {
        return taskService.updateStatus(principal.getId(),id,request.getStatus());
    }
    @PatchMapping("/{id}/complete") public TaskResponse complete(@AuthenticationPrincipal CustomUserDetails principal,@PathVariable Long id) { return taskService.complete(principal.getId(),id); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails principal,@PathVariable Long id) {
        taskService.delete(principal.getId(),id); return ResponseEntity.noContent().build();
    }
}

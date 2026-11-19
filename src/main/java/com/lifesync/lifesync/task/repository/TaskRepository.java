package com.lifesync.lifesync.task.repository;

import com.lifesync.lifesync.task.entity.Task;
import com.lifesync.lifesync.task.entity.TaskPriority;
import com.lifesync.lifesync.task.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    @EntityGraph(attributePaths = {"category", "goal", "milestone", "user"})
    Page<Task> findAll(org.springframework.data.jpa.domain.Specification<Task> specification, Pageable pageable);
    @EntityGraph(attributePaths = {"category", "goal", "milestone"})
    Optional<Task> findByIdAndUser_Id(Long id, Long userId);
    java.util.List<Task> findAllByUser_IdAndStartDateLessThanAndDueDateGreaterThan(Long userId, LocalDateTime end, LocalDateTime start);
    long countByUser_Id(Long userId);
    long countByUser_IdAndStatus(Long userId, TaskStatus status);
    long countByMilestone_IdAndUser_Id(Long milestoneId, Long userId);
    long countByMilestone_IdAndUser_IdAndStatus(Long milestoneId, Long userId, TaskStatus status);
    @Query("select count(t) from Task t where t.user.id = :userId and t.status = :status and (t.dueDate is null or t.dueDate >= :now)")
    long countNotOverdueByUserAndStatus(@Param("userId") Long userId, @Param("status") TaskStatus status, @Param("now") LocalDateTime now);
    long countByUser_IdAndPriorityIn(Long userId, java.util.Collection<TaskPriority> priorities);
    @Query("select count(t) from Task t where t.user.id = :userId and t.dueDate < :now and t.status not in :excluded")
    long countOverdue(@Param("userId") Long userId, @Param("now") LocalDateTime now, @Param("excluded") java.util.Collection<TaskStatus> excluded);
    @Query("select count(t) from Task t where t.user.id = :userId and t.dueDate >= :start and t.dueDate < :end")
    long countDueBetween(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    @Modifying
    @Query("update Task t set t.category = null where t.user.id = :userId and t.category.id = :categoryId")
    int clearCategory(@Param("userId") Long userId, @Param("categoryId") Long categoryId);
    @Modifying
    @Query("update Task t set t.milestone = null where t.user.id = :userId and t.milestone.id = :milestoneId")
    int clearMilestone(@Param("userId") Long userId, @Param("milestoneId") Long milestoneId);
    @Modifying
    @Query("update Task t set t.goal = null, t.milestone = null where t.user.id = :userId and t.goal.id = :goalId")
    int clearGoalReferences(@Param("userId") Long userId, @Param("goalId") Long goalId);
}

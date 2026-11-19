package com.lifesync.lifesync.goal.repository;
import com.lifesync.lifesync.goal.entity.Goal;
import com.lifesync.lifesync.goal.entity.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface GoalRepository extends JpaRepository<Goal,Long> {
    List<Goal> findAllByUser_IdOrderByTargetDateAsc(Long userId);
    Optional<Goal> findByIdAndUser_Id(Long id,Long userId);
    long countByUser_Id(Long userId);
    long countByUser_IdAndStatus(Long userId,GoalStatus status);
    long countByUser_IdAndTargetDateBeforeAndStatusNotIn(Long userId,LocalDate date,java.util.Collection<GoalStatus> excluded);
}

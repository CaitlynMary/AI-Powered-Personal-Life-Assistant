package com.lifesync.lifesync.milestone.repository;
import com.lifesync.lifesync.milestone.entity.GoalMilestone;
import com.lifesync.lifesync.milestone.entity.MilestoneStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface GoalMilestoneRepository extends JpaRepository<GoalMilestone,Long> {
    List<GoalMilestone> findAllByGoal_IdOrderByTargetDateAsc(Long goalId);
    List<GoalMilestone> findAllByGoal_Id(Long goalId);
    Optional<GoalMilestone> findByIdAndGoal_User_Id(Long id,Long userId);
    Optional<GoalMilestone> findByIdAndGoal_IdAndGoal_User_Id(Long id,Long goalId,Long userId);
    long countByGoal_Id(Long goalId);
    long countByGoal_IdAndStatus(Long goalId,MilestoneStatus status);
    @Modifying
    @Query("delete from GoalMilestone m where m.goal.id = :goalId")
    int deleteAllForGoal(@Param("goalId") Long goalId);
}

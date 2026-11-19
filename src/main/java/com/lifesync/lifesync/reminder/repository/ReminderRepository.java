package com.lifesync.lifesync.reminder.repository;
import com.lifesync.lifesync.reminder.entity.Reminder;
import com.lifesync.lifesync.reminder.entity.ReminderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
public interface ReminderRepository extends JpaRepository<Reminder,Long> {
    Page<Reminder> findAllByUser_Id(Long userId,Pageable pageable);
    Optional<Reminder> findByIdAndUser_Id(Long id,Long userId);
    List<Reminder> findAllByUser_IdAndTask_IdAndStatus(Long userId,Long taskId,ReminderStatus status);
    @Modifying
    @Query("update Reminder r set r.task = null, r.updatedAt = :updatedAt where r.task.id = :taskId")
    int clearTaskReferences(@Param("taskId") Long taskId, @Param("updatedAt") LocalDateTime updatedAt);
    Page<Reminder> findAllByUser_IdAndStatusAndReminderDateTimeGreaterThanEqual(Long userId,ReminderStatus status,LocalDateTime dateTime,Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Reminder> findTop100ByStatusAndReminderDateTimeLessThanEqualOrderByReminderDateTimeAsc(ReminderStatus status,LocalDateTime dateTime);
}

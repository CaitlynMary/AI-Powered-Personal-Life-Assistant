package com.lifesync.lifesync.notification.repository;
import com.lifesync.lifesync.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface NotificationRepository extends JpaRepository<Notification,Long> {
    Page<Notification> findAllByUser_IdOrderByCreatedAtDesc(Long userId,Pageable pageable);
    Page<Notification> findAllByUser_IdAndIsReadFalseOrderByCreatedAtDesc(Long userId,Pageable pageable);
    Optional<Notification> findByIdAndUser_Id(Long id,Long userId);
    boolean existsByReminder_Id(Long reminderId);
    boolean existsByEventKey(String eventKey);
    long countByUser_IdAndIsReadFalse(Long userId);
    @Modifying
    @Query("update Notification n set n.task = null where n.task.id = :taskId")
    int clearTaskReferences(@Param("taskId") Long taskId);
    @Modifying
    @Query("update Notification n set n.isRead = true where n.user.id = :userId and n.isRead = false")
    int markAllRead(@Param("userId") Long userId);
}

package com.lifesync.lifesync.schedule.repository;

import com.lifesync.lifesync.schedule.entity.ScheduleSlot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduleSlotRepository extends JpaRepository<ScheduleSlot, Long> {
    Optional<ScheduleSlot> findByIdAndUser_Id(Long id, Long userId);
    Page<ScheduleSlot> findAllByUser_Id(Long userId, Pageable pageable);
    List<ScheduleSlot> findAllByUser_IdAndTask_IdOrderByStartDateTimeAsc(Long userId, Long taskId);
    boolean existsByTask_Id(Long taskId);

    @Query("select s from ScheduleSlot s left join fetch s.task where s.user.id = :userId " +
            "and s.startDateTime < :rangeEnd and s.endDateTime > :rangeStart order by s.startDateTime")
    List<ScheduleSlot> findOverlappingRange(@Param("userId") Long userId,
                                            @Param("rangeStart") LocalDateTime rangeStart,
                                            @Param("rangeEnd") LocalDateTime rangeEnd);

    @Query("select s from ScheduleSlot s left join fetch s.task where s.user.id = :userId " +
            "and s.startDateTime < :rangeEnd and s.endDateTime > :rangeStart and s.id <> :excludedId order by s.startDateTime")
    List<ScheduleSlot> findOverlappingRangeExcludingId(@Param("userId") Long userId,
                                                       @Param("rangeStart") LocalDateTime rangeStart,
                                                       @Param("rangeEnd") LocalDateTime rangeEnd,
                                                       @Param("excludedId") Long excludedId);
}

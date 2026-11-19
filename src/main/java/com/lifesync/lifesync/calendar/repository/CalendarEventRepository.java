package com.lifesync.lifesync.calendar.repository;

import com.lifesync.lifesync.calendar.entity.CalendarEvent;
import com.lifesync.lifesync.calendar.entity.CalendarEventType;
import com.lifesync.lifesync.calendar.entity.CalendarRecurrenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {
    boolean existsByTask_Id(Long taskId);

    @Query("select e from CalendarEvent e left join fetch e.task where e.user.id = :userId " +
            "and (:eventType is null or e.eventType = :eventType) order by e.startDateTime")
    List<CalendarEvent> findAllForUser(@Param("userId") Long userId, @Param("eventType") CalendarEventType eventType);

    @Query("select e from CalendarEvent e left join fetch e.task where e.user.id = :userId " +
            "and (:eventType is null or e.eventType = :eventType) and " +
            "((e.recurrenceType = :none " +
            "and e.startDateTime < :rangeEnd and e.endDateTime > :rangeStart) " +
            "or (e.recurrenceType <> :none and e.startDateTime < :rangeEnd)) " +
            "order by e.startDateTime")
    List<CalendarEvent> findCandidatesForRange(@Param("userId") Long userId,
                                               @Param("eventType") CalendarEventType eventType,
                                               @Param("rangeStart") LocalDateTime rangeStart,
                                               @Param("rangeEnd") LocalDateTime rangeEnd,
                                               @Param("none") CalendarRecurrenceType none);

    @Query("select e from CalendarEvent e left join fetch e.task where e.id = :id and e.user.id = :userId")
    Optional<CalendarEvent> findOwnedById(@Param("id") Long id, @Param("userId") Long userId);
}

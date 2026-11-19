package com.lifesync.lifesync.habit.repository;
import com.lifesync.lifesync.habit.entity.HabitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface HabitLogRepository extends JpaRepository<HabitLog,Long> {
    Optional<HabitLog> findByHabit_IdAndCompletionDate(Long habitId,LocalDate completionDate);
    boolean existsByHabit_IdAndCompletionDate(Long habitId,LocalDate completionDate);
    long countByHabit_IdAndCompletedTrueAndCompletionDateBetween(Long habitId,LocalDate start,LocalDate end);
    List<HabitLog> findAllByHabit_IdOrderByCompletionDateDesc(Long habitId);
    List<HabitLog> findAllByHabit_IdAndCompletionDateBetweenOrderByCompletionDateAsc(Long habitId,LocalDate start,LocalDate end);
    List<HabitLog> findAllByHabit_IdAndCompletedTrueAndCompletionDateLessThanEqualOrderByCompletionDateAsc(Long habitId,LocalDate date);
    List<HabitLog> findAllByHabit_User_IdAndCompletionDateBetween(Long userId,LocalDate start,LocalDate end);
    List<HabitLog> findAllByHabit_User_Id(Long userId);
    @Modifying
    @Query("delete from HabitLog log where log.habit.id = :habitId")
    int deleteAllByHabit_Id(@Param("habitId") Long habitId);
}

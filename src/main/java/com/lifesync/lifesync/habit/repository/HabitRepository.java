package com.lifesync.lifesync.habit.repository;
import com.lifesync.lifesync.habit.entity.Habit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface HabitRepository extends JpaRepository<Habit,Long> {
    List<Habit> findAllByUser_IdOrderByNameAsc(Long userId);
    Optional<Habit> findByIdAndUser_Id(Long id,Long userId);
    long countByUser_Id(Long userId);
    long countByUser_IdAndActiveTrue(Long userId);
}

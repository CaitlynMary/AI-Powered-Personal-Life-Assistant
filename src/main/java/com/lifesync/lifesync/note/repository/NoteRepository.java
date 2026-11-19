package com.lifesync.lifesync.note.repository;
import com.lifesync.lifesync.note.entity.Note;
import com.lifesync.lifesync.note.entity.NoteType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.time.LocalDate;
import java.util.Optional;
public interface NoteRepository extends JpaRepository<Note,Long>,JpaSpecificationExecutor<Note> {
    Optional<Note> findByIdAndUser_Id(Long id,Long userId);
    Optional<Note> findByUser_IdAndNoteTypeAndJournalDate(Long userId,NoteType type,LocalDate date);
    boolean existsByUser_IdAndNoteTypeAndJournalDate(Long userId,NoteType type,LocalDate date);
    boolean existsByUser_IdAndNoteTypeAndJournalDateAndIdNot(Long userId,NoteType type,LocalDate date,Long id);
    Page<Note> findAll(Specification<Note> specification,Pageable pageable);
    long countByUser_Id(Long userId);
    long countByUser_IdAndNoteType(Long userId,NoteType type);
    long countByUser_IdAndPinnedTrue(Long userId);
}

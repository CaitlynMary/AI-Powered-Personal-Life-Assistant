package com.lifesync.lifesync.note.service;

import com.lifesync.lifesync.exception.BadRequestException;
import com.lifesync.lifesync.exception.DuplicateResourceException;
import com.lifesync.lifesync.exception.ResourceNotFoundException;
import com.lifesync.lifesync.note.dto.NoteRequest;
import com.lifesync.lifesync.note.entity.Note;
import com.lifesync.lifesync.note.entity.NoteType;
import com.lifesync.lifesync.note.repository.NoteRepository;
import com.lifesync.lifesync.note.service.impl.NoteServiceImpl;
import com.lifesync.lifesync.user.entity.User;
import com.lifesync.lifesync.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceImplTest {
    @Mock NoteRepository notes;
    @Mock UserRepository users;
    private NoteServiceImpl service;
    private User owner;
    @BeforeEach void setUp(){service=new NoteServiceImpl(notes,users);owner=new User();owner.setId(3L);}

    @Test void createsGeneralAndJournalNotesForAuthenticatedOwner() {
        when(users.findById(3L)).thenReturn(Optional.of(owner));
        when(notes.save(any(Note.class))).thenAnswer(i->{Note note=i.getArgument(0);note.setId(10L);return note;});
        var general=service.create(3L,request(NoteType.GENERAL,null));
        assertEquals(NoteType.GENERAL,general.getNoteType());assertNull(general.getJournalDate());
        LocalDate date=LocalDate.of(2026,11,4);
        when(notes.existsByUser_IdAndNoteTypeAndJournalDate(3L,NoteType.JOURNAL,date)).thenReturn(false);
        var journal=service.create(3L,request(NoteType.JOURNAL,date));
        assertEquals(date,journal.getJournalDate());
        verify(notes,times(2)).save(any(Note.class));
    }

    @Test void rejectsDuplicateJournalEntryForUserAndDate() {
        LocalDate date=LocalDate.of(2026,11,4);
        when(users.findById(3L)).thenReturn(Optional.of(owner));
        when(notes.existsByUser_IdAndNoteTypeAndJournalDate(3L,NoteType.JOURNAL,date)).thenReturn(true);
        assertThrows(DuplicateResourceException.class,()->service.create(3L,request(NoteType.JOURNAL,date)));
        verify(notes,never()).save(any(Note.class));
    }

    @Test void validatesJournalDateAndNonEmptyContent() {
        assertThrows(BadRequestException.class,()->service.create(3L,request(NoteType.JOURNAL,null)));
        NoteRequest empty=new NoteRequest(" "," ",NoteType.GENERAL,null);
        assertThrows(BadRequestException.class,()->service.create(3L,empty));
    }

    @Test void getsAndUpdatesOnlyOwnedNote() {
        Note note=note(8L,NoteType.GENERAL,null);when(notes.findByIdAndUser_Id(8L,3L)).thenReturn(Optional.of(note));
        when(notes.existsByUser_IdAndNoteTypeAndJournalDateAndIdNot(3L,NoteType.JOURNAL,LocalDate.of(2026,11,3),8L)).thenReturn(false);
        when(notes.save(note)).thenReturn(note);
        assertEquals("Note",service.getById(3L,8L).getTitle());
        var updated=service.update(3L,8L,request(NoteType.JOURNAL,LocalDate.of(2026,11,3)));
        assertEquals(NoteType.JOURNAL,updated.getNoteType());assertEquals(8L,updated.getId());
        when(notes.findByIdAndUser_Id(99L,3L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->service.getById(3L,99L));
    }

    @Test void searchesAndPaginatesWithinOwner() {
        when(notes.findAll(any(Specification.class),any(Pageable.class))).thenReturn(new PageImpl<>(List.of(note(1L,NoteType.GENERAL,null))));
        var page=service.getAll(3L,0,10,"search text","updatedAt","DESC");
        assertEquals(1,page.getTotalElements());assertEquals(1,page.getContent().size());
        verify(notes).findAll(any(Specification.class),any(Pageable.class));
        assertThrows(BadRequestException.class,()->service.getAll(3L,0,20,null,"password","DESC"));
    }

    @Test void journalLookupAndStatisticsAreUserScoped() {
        LocalDate date=LocalDate.of(2026,11,4);Note journal=note(2L,NoteType.JOURNAL,date);
        when(notes.findByUser_IdAndNoteTypeAndJournalDate(3L,NoteType.JOURNAL,date)).thenReturn(Optional.of(journal));
        assertEquals(1,service.getJournalByDate(3L,date).size());
        when(notes.countByUser_Id(3L)).thenReturn(4L);when(notes.countByUser_IdAndNoteType(3L,NoteType.GENERAL)).thenReturn(2L);
        when(notes.countByUser_IdAndNoteType(3L,NoteType.JOURNAL)).thenReturn(2L);when(notes.countByUser_IdAndPinnedTrue(3L)).thenReturn(1L);
        var stats=service.statistics(3L);assertEquals(4L,stats.getTotalNotes());assertEquals(2L,stats.getJournalEntries());
    }

    @Test void togglesPinAndDeletesOwnedNote() {
        Note note=note(8L,NoteType.GENERAL,null);when(notes.findByIdAndUser_Id(8L,3L)).thenReturn(Optional.of(note));when(notes.save(note)).thenReturn(note);
        assertTrue(service.togglePin(3L,8L).getPinned());assertFalse(service.togglePin(3L,8L).getPinned());
        service.delete(3L,8L);verify(notes).delete(note);
    }

    private NoteRequest request(NoteType type,LocalDate date){return new NoteRequest("Note","Some content",type,date);}
    private Note note(Long id,NoteType type,LocalDate date){Note note=new Note();note.setId(id);note.setUser(owner);note.setTitle("Note");note.setContent("Content");note.setNoteType(type);note.setPinned(false);note.setJournalDate(date);return note;}
}

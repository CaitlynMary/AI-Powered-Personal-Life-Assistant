package com.lifesync.lifesync.note.service;
import com.lifesync.lifesync.note.dto.*;
import java.time.LocalDate;
import java.util.List;
public interface NoteService {
    NoteResponse create(Long userId,NoteRequest request);
    NotePageResponse getAll(Long userId,int page,int size,String search,String sortBy,String direction);
    NoteResponse getById(Long userId,Long id);
    NoteResponse update(Long userId,Long id,NoteRequest request);
    void delete(Long userId,Long id);
    NoteResponse togglePin(Long userId,Long id);
    List<NoteResponse> getJournalByDate(Long userId,LocalDate date);
    NoteStatisticsResponse statistics(Long userId);
}

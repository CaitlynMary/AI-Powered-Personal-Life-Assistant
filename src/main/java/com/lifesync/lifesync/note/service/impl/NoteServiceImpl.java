package com.lifesync.lifesync.note.service.impl;

import com.lifesync.lifesync.common.ApiConstants;
import com.lifesync.lifesync.exception.BadRequestException;
import com.lifesync.lifesync.exception.DuplicateResourceException;
import com.lifesync.lifesync.exception.ResourceNotFoundException;
import com.lifesync.lifesync.note.dto.*;
import com.lifesync.lifesync.note.entity.Note;
import com.lifesync.lifesync.note.entity.NoteType;
import com.lifesync.lifesync.note.repository.NoteRepository;
import com.lifesync.lifesync.note.service.NoteService;
import com.lifesync.lifesync.user.entity.User;
import com.lifesync.lifesync.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class NoteServiceImpl implements NoteService {
    private static final Set<String> SORT_FIELDS=Set.of("createdAt","updatedAt","title","noteType","pinned","journalDate");
    private final NoteRepository notes;
    private final UserRepository users;
    public NoteServiceImpl(NoteRepository notes,UserRepository users){this.notes=notes;this.users=users;}

    @Override @Transactional
    public NoteResponse create(Long userId,NoteRequest request){
        validateTypeAndDate(request);
        User user=users.findById(userId).orElseThrow(()->new ResourceNotFoundException("User account was not found"));
        if(isDuplicateJournal(userId,request.getNoteType(),request.getJournalDate(),null))throw new DuplicateResourceException("A journal entry already exists for this date");
        Note note=new Note();note.setUser(user);apply(note,request);
        try{return response(notes.save(note));}catch(DataIntegrityViolationException ex){throw duplicateJournalIfApplicable(request.getNoteType());}
    }
    @Override @Transactional(readOnly=true)
    public NotePageResponse getAll(Long userId,int page,int size,String search,String sortBy,String direction){
        if(page<0)throw new BadRequestException("Page must be zero or greater");
        if(size<1||size>ApiConstants.MAX_PAGE_SIZE)throw new BadRequestException("Page size must be between 1 and " + ApiConstants.MAX_PAGE_SIZE);
        if(!SORT_FIELDS.contains(sortBy))throw new BadRequestException("Unsupported sort field");
        Sort.Direction sortDirection;
        try{sortDirection=Sort.Direction.fromString(direction);}catch(IllegalArgumentException ex){throw new BadRequestException("Direction must be ASC or DESC");}
        Specification<Note> specification=(root,query,cb)->cb.equal(root.get("user").get("id"),userId);
        if(search!=null&&!search.isBlank()){
            String term="%"+search.trim().toLowerCase(java.util.Locale.ROOT)+"%";
            specification=specification.and((root,query,cb)->cb.or(cb.like(cb.lower(root.get("title")),term),cb.like(cb.lower(root.get("content")),term)));
        }
        Page<Note> result=notes.findAll(specification,PageRequest.of(page,size,Sort.by(sortDirection,sortBy)));
        return new NotePageResponse(result.getContent().stream().map(this::response).toList(),result.getNumber(),result.getSize(),result.getTotalElements(),result.getTotalPages(),result.isLast());
    }
    @Override @Transactional(readOnly=true)
    public NoteResponse getById(Long userId,Long id){return response(findNote(userId,id));}
    @Override @Transactional
    public NoteResponse update(Long userId,Long id,NoteRequest request){
        validateTypeAndDate(request);Note note=findNote(userId,id);
        if(isDuplicateJournal(userId,request.getNoteType(),request.getJournalDate(),id))throw new DuplicateResourceException("A journal entry already exists for this date");
        apply(note,request);
        try{return response(notes.save(note));}catch(DataIntegrityViolationException ex){throw duplicateJournalIfApplicable(request.getNoteType());}
    }
    @Override @Transactional
    public void delete(Long userId,Long id){notes.delete(findNote(userId,id));}
    @Override @Transactional
    public NoteResponse togglePin(Long userId,Long id){Note note=findNote(userId,id);note.setPinned(!Boolean.TRUE.equals(note.getPinned()));return response(notes.save(note));}
    @Override @Transactional(readOnly=true)
    public List<NoteResponse> getJournalByDate(Long userId,LocalDate date){
        if(date==null)throw new BadRequestException("date is required");
        return notes.findByUser_IdAndNoteTypeAndJournalDate(userId,NoteType.JOURNAL,date).stream().map(this::response).toList();
    }
    @Override @Transactional(readOnly=true)
    public NoteStatisticsResponse statistics(Long userId){return new NoteStatisticsResponse(notes.countByUser_Id(userId),notes.countByUser_IdAndNoteType(userId,NoteType.GENERAL),notes.countByUser_IdAndNoteType(userId,NoteType.JOURNAL),notes.countByUser_IdAndPinnedTrue(userId));}

    private Note findNote(Long userId,Long id){return notes.findByIdAndUser_Id(id,userId).orElseThrow(()->new ResourceNotFoundException("Note was not found"));}
    private boolean isDuplicateJournal(Long userId,NoteType type,LocalDate date,Long excludeId){
        if(type!=NoteType.JOURNAL||date==null)return false;
        return excludeId==null?notes.existsByUser_IdAndNoteTypeAndJournalDate(userId,type,date):notes.existsByUser_IdAndNoteTypeAndJournalDateAndIdNot(userId,type,date,excludeId);
    }
    private void validateTypeAndDate(NoteRequest request){
        if(request.getNoteType()==null)throw new BadRequestException("noteType is required");
        if(request.getNoteType()==NoteType.JOURNAL&&request.getJournalDate()==null)throw new BadRequestException("journalDate is required for journal entries");
        if(request.getNoteType()==NoteType.GENERAL&&request.getJournalDate()!=null)throw new BadRequestException("journalDate is only allowed for journal entries");
        if(request.getTitle()==null||request.getTitle().isBlank())throw new BadRequestException("title is required");
        if(request.getContent()==null||request.getContent().isBlank())throw new BadRequestException("content is required");
    }
    private void apply(Note note,NoteRequest request){
        note.setTitle(request.getTitle().trim());note.setContent(request.getContent().trim());note.setNoteType(request.getNoteType());
        note.setJournalDate(request.getNoteType()==NoteType.JOURNAL?request.getJournalDate():null);
    }
    private DuplicateResourceException duplicateJournalIfApplicable(NoteType type){
        if(type==NoteType.JOURNAL)return new DuplicateResourceException("A journal entry already exists for this date");
        return new DuplicateResourceException("The note conflicts with an existing record");
    }
    private NoteResponse response(Note n){return new NoteResponse(n.getId(),n.getTitle(),n.getContent(),n.getNoteType(),n.getPinned(),n.getJournalDate(),n.getCreatedAt(),n.getUpdatedAt());}
}

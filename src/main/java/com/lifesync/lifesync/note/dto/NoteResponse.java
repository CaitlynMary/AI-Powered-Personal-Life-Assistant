package com.lifesync.lifesync.note.dto;
import com.lifesync.lifesync.note.entity.NoteType;
import java.time.LocalDate;
import java.time.LocalDateTime;
public class NoteResponse {
    private Long id; private String title; private String content; private NoteType noteType; private Boolean pinned; private LocalDate journalDate; private LocalDateTime createdAt; private LocalDateTime updatedAt;
    public NoteResponse() { }
    public NoteResponse(Long id,String title,String content,NoteType noteType,Boolean pinned,LocalDate journalDate,LocalDateTime createdAt,LocalDateTime updatedAt){this.id=id;this.title=title;this.content=content;this.noteType=noteType;this.pinned=pinned;this.journalDate=journalDate;this.createdAt=createdAt;this.updatedAt=updatedAt;}
    public Long getId(){return id;} public void setId(Long v){id=v;} public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getContent(){return content;} public void setContent(String v){content=v;} public NoteType getNoteType(){return noteType;} public void setNoteType(NoteType v){noteType=v;}
    public Boolean getPinned(){return pinned;} public void setPinned(Boolean v){pinned=v;} public LocalDate getJournalDate(){return journalDate;} public void setJournalDate(LocalDate v){journalDate=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;} public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
}

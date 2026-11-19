package com.lifesync.lifesync.note.dto;
import com.lifesync.lifesync.note.entity.NoteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
public class NoteRequest {
    @NotBlank @Size(max=200) private String title;
    @NotBlank @Size(max=50000) private String content;
    @NotNull private NoteType noteType;
    private LocalDate journalDate;
    public NoteRequest() { }
    public NoteRequest(String title,String content,NoteType noteType,LocalDate journalDate){this.title=title;this.content=content;this.noteType=noteType;this.journalDate=journalDate;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getContent(){return content;} public void setContent(String v){content=v;}
    public NoteType getNoteType(){return noteType;} public void setNoteType(NoteType v){noteType=v;}
    public LocalDate getJournalDate(){return journalDate;} public void setJournalDate(LocalDate v){journalDate=v;}
}

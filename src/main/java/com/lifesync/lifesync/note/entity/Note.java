package com.lifesync.lifesync.note.entity;

import com.lifesync.lifesync.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="notes",uniqueConstraints=@UniqueConstraint(name="uk_note_user_journal_date",columnNames={"user_id","journal_date"}),
        indexes={@Index(name="idx_note_user_updated",columnList="user_id, updated_at"),@Index(name="idx_note_user_type",columnList="user_id, note_type")})
public class Note {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=200) private String title;
    @Column(nullable=false,columnDefinition="text") private String content;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false,foreignKey=@ForeignKey(name="fk_note_user")) private User user;
    @Enumerated(EnumType.STRING) @Column(name="note_type",nullable=false,length=20) private NoteType noteType;
    @Column(nullable=false) private Boolean pinned=false;
    @Column(name="journal_date") private LocalDate journalDate;
    @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
    @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
    public Note() { }
    @PrePersist public void onCreate(){LocalDateTime now=LocalDateTime.now();createdAt=now;updatedAt=now;}
    @PreUpdate public void onUpdate(){updatedAt=LocalDateTime.now();}
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getContent(){return content;} public void setContent(String v){content=v;}
    public User getUser(){return user;} public void setUser(User v){user=v;}
    public NoteType getNoteType(){return noteType;} public void setNoteType(NoteType v){noteType=v;}
    public Boolean getPinned(){return pinned;} public void setPinned(Boolean v){pinned=v;}
    public LocalDate getJournalDate(){return journalDate;} public void setJournalDate(LocalDate v){journalDate=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
}

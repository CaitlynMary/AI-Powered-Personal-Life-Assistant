package com.lifesync.lifesync.note.dto;
public class NoteStatisticsResponse {
    private long totalNotes; private long generalNotes; private long journalEntries; private long pinnedNotes;
    public NoteStatisticsResponse() { }
    public NoteStatisticsResponse(long totalNotes,long generalNotes,long journalEntries,long pinnedNotes){this.totalNotes=totalNotes;this.generalNotes=generalNotes;this.journalEntries=journalEntries;this.pinnedNotes=pinnedNotes;}
    public long getTotalNotes(){return totalNotes;} public void setTotalNotes(long v){totalNotes=v;}
    public long getGeneralNotes(){return generalNotes;} public void setGeneralNotes(long v){generalNotes=v;}
    public long getJournalEntries(){return journalEntries;} public void setJournalEntries(long v){journalEntries=v;}
    public long getPinnedNotes(){return pinnedNotes;} public void setPinnedNotes(long v){pinnedNotes=v;}
}

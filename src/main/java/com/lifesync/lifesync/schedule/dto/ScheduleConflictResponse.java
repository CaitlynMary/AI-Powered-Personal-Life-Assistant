package com.lifesync.lifesync.schedule.dto;

import java.util.List;

public class ScheduleConflictResponse {
    private boolean hasConflicts;
    private List<ScheduleConflictItem> conflicts;
    public ScheduleConflictResponse() { }
    public ScheduleConflictResponse(boolean hasConflicts, List<ScheduleConflictItem> conflicts) { this.hasConflicts=hasConflicts; this.conflicts=conflicts; }
    public boolean isHasConflicts() { return hasConflicts; }
    public void setHasConflicts(boolean hasConflicts) { this.hasConflicts=hasConflicts; }
    public List<ScheduleConflictItem> getConflicts() { return conflicts; }
    public void setConflicts(List<ScheduleConflictItem> conflicts) { this.conflicts=conflicts; }
}

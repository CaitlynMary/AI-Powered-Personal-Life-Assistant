package com.lifesync.lifesync.ai.dto;

import java.time.LocalTime;

/** Internal, untrusted values returned by the AI provider. */
public class AiPlanItemDraft {
    private Long taskId;
    private LocalTime startTime;
    private LocalTime endTime;
    private String rationale;
    public AiPlanItemDraft() { }
    public AiPlanItemDraft(Long taskId, LocalTime startTime, LocalTime endTime, String rationale) {
        this.taskId=taskId; this.startTime=startTime; this.endTime=endTime; this.rationale=rationale;
    }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId=taskId; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime=startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime=endTime; }
    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale=rationale; }
}

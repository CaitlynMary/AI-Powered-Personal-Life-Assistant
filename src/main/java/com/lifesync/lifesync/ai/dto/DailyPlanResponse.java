package com.lifesync.lifesync.ai.dto;

import java.time.LocalDate;
import java.util.List;

public class DailyPlanResponse {
    private LocalDate date;
    private String summary;
    private List<DailyTimeWindow> availableWindows;
    private List<DailyPlanSuggestion> suggestions;
    private List<String> warnings;
    public DailyPlanResponse() { }
    public DailyPlanResponse(LocalDate date, String summary, List<DailyTimeWindow> availableWindows,
                             List<DailyPlanSuggestion> suggestions, List<String> warnings) {
        this.date=date; this.summary=summary; this.availableWindows=availableWindows; this.suggestions=suggestions; this.warnings=warnings;
    }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date=date; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary=summary; }
    public List<DailyTimeWindow> getAvailableWindows() { return availableWindows; }
    public void setAvailableWindows(List<DailyTimeWindow> availableWindows) { this.availableWindows=availableWindows; }
    public List<DailyPlanSuggestion> getSuggestions() { return suggestions; }
    public void setSuggestions(List<DailyPlanSuggestion> suggestions) { this.suggestions=suggestions; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings=warnings; }
}

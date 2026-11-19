package com.lifesync.lifesync.ai.dto;

import java.util.List;

/** Internal structured-output target for provider-generated plan fields. */
public class AiPlanDraft {
    private String summary;
    private List<AiPlanItemDraft> suggestions;
    public AiPlanDraft() { }
    public AiPlanDraft(String summary, List<AiPlanItemDraft> suggestions) { this.summary=summary; this.suggestions=suggestions; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary=summary; }
    public List<AiPlanItemDraft> getSuggestions() { return suggestions; }
    public void setSuggestions(List<AiPlanItemDraft> suggestions) { this.suggestions=suggestions; }
}

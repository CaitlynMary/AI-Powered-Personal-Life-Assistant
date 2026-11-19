package com.lifesync.lifesync.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

public class AiChatRequest {
    @NotBlank(message = "message is required")
    @Size(max = 4000, message = "message must be 4000 characters or fewer")
    private String message;

    @Size(max = 12, message = "history must contain 12 messages or fewer")
    private List<@Valid AiChatMessage> history = new ArrayList<>();

    public AiChatRequest() { }
    public AiChatRequest(String message) { this.message=message; }
    public AiChatRequest(String message, List<AiChatMessage> history) {
        this.message = message;
        this.history = history == null ? new ArrayList<>() : new ArrayList<>(history);
    }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message=message; }
    public List<AiChatMessage> getHistory() { return history; }
    public void setHistory(List<AiChatMessage> history) { this.history = history; }
}

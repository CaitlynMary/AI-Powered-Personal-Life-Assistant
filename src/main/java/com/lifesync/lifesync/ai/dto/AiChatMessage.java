package com.lifesync.lifesync.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AiChatMessage {
    public enum Role { USER, ASSISTANT }

    @NotNull
    private Role role;

    @NotBlank
    @Size(max = 4000)
    private String content;

    public AiChatMessage() { }

    public AiChatMessage(Role role, String content) {
        this.role = role;
        this.content = content;
    }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}

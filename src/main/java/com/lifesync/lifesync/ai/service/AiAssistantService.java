package com.lifesync.lifesync.ai.service;

import com.lifesync.lifesync.ai.dto.DailyPlanRequest;
import com.lifesync.lifesync.ai.dto.DailyPlanResponse;
import com.lifesync.lifesync.ai.dto.AiChatMessage;
import java.util.List;

public interface AiAssistantService {
    String chat(Long userId, String message, List<AiChatMessage> history);
    String chat(Long userId, String message, List<AiChatMessage> history,
                String responseStyle, boolean includePersonalization);
    DailyPlanResponse createDailyPlan(Long userId, DailyPlanRequest request);
}

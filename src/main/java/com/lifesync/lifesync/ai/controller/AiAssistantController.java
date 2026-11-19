package com.lifesync.lifesync.ai.controller;

import com.lifesync.lifesync.ai.dto.AiChatRequest;
import com.lifesync.lifesync.ai.dto.DailyBriefingResponse;
import com.lifesync.lifesync.ai.dto.DailyPlanRequest;
import com.lifesync.lifesync.ai.service.AiAssistantService;
import com.lifesync.lifesync.ai.service.DailyBriefingService;
import com.lifesync.lifesync.common.ApiResponse;
import com.lifesync.lifesync.security.principal.CustomUserDetails;
import com.lifesync.lifesync.user.dto.UserSettingsResponse;
import com.lifesync.lifesync.user.service.UserSettingsService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiAssistantController {
    private final AiAssistantService aiAssistantService;
    private final DailyBriefingService dailyBriefingService;
    private final UserSettingsService userSettings;

    public AiAssistantController(AiAssistantService aiAssistantService, DailyBriefingService dailyBriefingService,
                                 UserSettingsService userSettings) {
        this.aiAssistantService = aiAssistantService;
        this.dailyBriefingService = dailyBriefingService;
        this.userSettings = userSettings;
    }

    @GetMapping("/daily-briefing")
    public DailyBriefingResponse dailyBriefing(@AuthenticationPrincipal CustomUserDetails principal) {
        return dailyBriefingService.today(principal.getId());
    }

    @PostMapping("/chat")
    public ApiResponse<String> chat(@AuthenticationPrincipal CustomUserDetails principal,
                                    @Valid @RequestBody AiChatRequest request) {
        UserSettingsResponse settings = userSettings.getForUser(principal.getId());
        return ApiResponse.success("AI response generated",
                aiAssistantService.chat(principal.getId(), request.getMessage(), request.getHistory(),
                        settings.aiResponseStyle(), settings.aiPersonalizationEnabled()));
    }

    @PostMapping("/daily-plan")
    public ApiResponse<com.lifesync.lifesync.ai.dto.DailyPlanResponse> dailyPlan(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody DailyPlanRequest request) {
        return ApiResponse.success("Daily plan suggestions are ready for review",
                aiAssistantService.createDailyPlan(principal.getId(), request));
    }
}

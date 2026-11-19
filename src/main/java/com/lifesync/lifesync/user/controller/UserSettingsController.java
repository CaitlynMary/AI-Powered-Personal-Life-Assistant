package com.lifesync.lifesync.user.controller;

import com.lifesync.lifesync.security.principal.CustomUserDetails;
import com.lifesync.lifesync.user.dto.UserSettingsRequest;
import com.lifesync.lifesync.user.dto.UserSettingsResponse;
import com.lifesync.lifesync.user.service.UserSettingsService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/settings")
public class UserSettingsController {
    private final UserSettingsService settings;

    public UserSettingsController(UserSettingsService settings) {
        this.settings = settings;
    }

    @GetMapping
    public UserSettingsResponse get(@AuthenticationPrincipal CustomUserDetails principal) {
        return settings.getForUser(principal.getId());
    }

    @PutMapping
    public UserSettingsResponse update(@AuthenticationPrincipal CustomUserDetails principal,
                                       @Valid @RequestBody UserSettingsRequest request) {
        return settings.updateForUser(principal.getId(), request);
    }
}

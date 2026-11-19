package com.lifesync.lifesync.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserSettingsRequest(
        boolean taskRemindersEnabled,
        boolean goalMilestoneUpdatesEnabled,
        boolean habitRemindersEnabled,
        boolean aiPlannerUpdatesEnabled,
        @NotBlank @Size(max = 100) String timeZoneId,
        @NotBlank @Pattern(regexp = "LOCALE|MM_DD_YYYY|DD_MM_YYYY|YYYY_MM_DD") String dateFormat,
        @NotBlank @Pattern(regexp = "LOCALE|H12|H24") String timeFormat,
        @NotBlank @Pattern(regexp = "SUNDAY|MONDAY") String weekStart,
        @NotBlank @Pattern(regexp = "CONCISE|BALANCED|DETAILED") String aiResponseStyle,
        boolean aiPersonalizationEnabled
) {}

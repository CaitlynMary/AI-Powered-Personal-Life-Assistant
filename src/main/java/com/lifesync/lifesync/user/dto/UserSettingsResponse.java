package com.lifesync.lifesync.user.dto;

import com.lifesync.lifesync.user.entity.UserSettings;

public record UserSettingsResponse(
        boolean taskRemindersEnabled,
        boolean goalMilestoneUpdatesEnabled,
        boolean habitRemindersEnabled,
        boolean aiPlannerUpdatesEnabled,
        String timeZoneId,
        String dateFormat,
        String timeFormat,
        String weekStart,
        String aiResponseStyle,
        boolean aiPersonalizationEnabled
) {
    public static UserSettingsResponse from(UserSettings settings) {
        return new UserSettingsResponse(
                settings.isTaskRemindersEnabled(),
                settings.isGoalMilestoneUpdatesEnabled(),
                settings.isHabitRemindersEnabled(),
                settings.isAiPlannerUpdatesEnabled(),
                settings.getTimeZoneId(),
                settings.getDateFormat(),
                settings.getTimeFormat(),
                settings.getWeekStart(),
                settings.getAiResponseStyle(),
                settings.isAiPersonalizationEnabled());
    }

    public static UserSettingsResponse defaults() {
        return new UserSettingsResponse(true, true, true, true, "DEVICE", "LOCALE", "LOCALE",
                "MONDAY", "BALANCED", true);
    }
}

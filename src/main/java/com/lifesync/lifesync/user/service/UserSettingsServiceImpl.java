package com.lifesync.lifesync.user.service;

import com.lifesync.lifesync.exception.BadRequestException;
import com.lifesync.lifesync.exception.ResourceNotFoundException;
import com.lifesync.lifesync.notification.entity.NotificationType;
import com.lifesync.lifesync.user.dto.UserSettingsRequest;
import com.lifesync.lifesync.user.dto.UserSettingsResponse;
import com.lifesync.lifesync.user.entity.User;
import com.lifesync.lifesync.user.entity.UserSettings;
import com.lifesync.lifesync.user.repository.UserRepository;
import com.lifesync.lifesync.user.repository.UserSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;

@Service
public class UserSettingsServiceImpl implements UserSettingsService {
    private final UserRepository users;
    private final UserSettingsRepository settings;

    public UserSettingsServiceImpl(UserRepository users, UserSettingsRepository settings) {
        this.users = users;
        this.settings = settings;
    }

    @Override
    @Transactional(readOnly = true)
    public UserSettingsResponse getForUser(Long userId) {
        return settings.findByUser_Id(userId)
                .map(UserSettingsResponse::from)
                .orElseGet(UserSettingsResponse::defaults);
    }

    @Override
    @Transactional
    public UserSettingsResponse updateForUser(Long userId, UserSettingsRequest request) {
        User user = users.findById(userId)
                .filter(account -> Boolean.TRUE.equals(account.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException("User account was not found"));
        String timeZoneId = normalizeTimeZone(request.timeZoneId());
        UserSettings preference = settings.findByUser_Id(userId).orElseGet(() -> {
            UserSettings created = new UserSettings();
            created.setUser(user);
            return created;
        });
        preference.setTaskRemindersEnabled(request.taskRemindersEnabled());
        preference.setGoalMilestoneUpdatesEnabled(request.goalMilestoneUpdatesEnabled());
        preference.setHabitRemindersEnabled(request.habitRemindersEnabled());
        preference.setAiPlannerUpdatesEnabled(request.aiPlannerUpdatesEnabled());
        preference.setTimeZoneId(timeZoneId);
        preference.setDateFormat(request.dateFormat());
        preference.setTimeFormat(request.timeFormat());
        preference.setWeekStart(request.weekStart());
        preference.setAiResponseStyle(request.aiResponseStyle());
        preference.setAiPersonalizationEnabled(request.aiPersonalizationEnabled());
        return UserSettingsResponse.from(settings.save(preference));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean notificationEnabled(Long userId, NotificationType type) {
        UserSettingsResponse preference = getForUser(userId);
        return switch (type) {
            case REMINDER, TASK_DEADLINE_APPROACHING, TASK_OVERDUE, TASK_COMPLETED,
                    TASK_MISSED, TASK_RESCHEDULED -> preference.taskRemindersEnabled();
            case MILESTONE_COMPLETED -> preference.goalMilestoneUpdatesEnabled();
            case HABIT_COMPLETED -> preference.habitRemindersEnabled();
            case AI_DAILY_PLAN_READY -> preference.aiPlannerUpdatesEnabled();
            case SYSTEM -> true;
        };
    }

    private String normalizeTimeZone(String value) {
        String timeZoneId = value.trim();
        if ("DEVICE".equalsIgnoreCase(timeZoneId)) return "DEVICE";
        try {
            ZoneId.of(timeZoneId);
            return timeZoneId;
        } catch (DateTimeException exception) {
            throw new BadRequestException("timeZoneId must be DEVICE or a valid IANA time zone");
        }
    }
}

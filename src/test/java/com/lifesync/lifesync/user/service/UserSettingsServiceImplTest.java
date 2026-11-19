package com.lifesync.lifesync.user.service;

import com.lifesync.lifesync.exception.BadRequestException;
import com.lifesync.lifesync.notification.entity.NotificationType;
import com.lifesync.lifesync.user.dto.UserSettingsRequest;
import com.lifesync.lifesync.user.dto.UserSettingsResponse;
import com.lifesync.lifesync.user.entity.User;
import com.lifesync.lifesync.user.entity.UserSettings;
import com.lifesync.lifesync.user.repository.UserRepository;
import com.lifesync.lifesync.user.repository.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserSettingsServiceImplTest {
    private UserRepository users;
    private UserSettingsRepository settings;
    private UserSettingsServiceImpl service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        settings = mock(UserSettingsRepository.class);
        service = new UserSettingsServiceImpl(users, settings);
    }

    @Test
    void returnsSafeDefaultsWhenUserHasNoSavedSettings() {
        when(settings.findByUser_Id(17L)).thenReturn(Optional.empty());

        UserSettingsResponse response = service.getForUser(17L);

        assertTrue(response.taskRemindersEnabled());
        assertTrue(response.goalMilestoneUpdatesEnabled());
        assertTrue(response.habitRemindersEnabled());
        assertTrue(response.aiPlannerUpdatesEnabled());
        assertEquals("DEVICE", response.timeZoneId());
        assertEquals("MONDAY", response.weekStart());
        assertEquals("BALANCED", response.aiResponseStyle());
        assertTrue(response.aiPersonalizationEnabled());
    }

    @Test
    void persistsValidatedPreferencesForTheAuthenticatedUser() {
        User user = activeUser(17L);
        when(users.findById(17L)).thenReturn(Optional.of(user));
        when(settings.findByUser_Id(17L)).thenReturn(Optional.empty());
        when(settings.save(any(UserSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UserSettingsRequest request = new UserSettingsRequest(false, true, false, true,
                " Asia/Kolkata ", "DD_MM_YYYY", "H24", "SUNDAY", "DETAILED", false);

        UserSettingsResponse response = service.updateForUser(17L, request);

        assertFalse(response.taskRemindersEnabled());
        assertFalse(response.habitRemindersEnabled());
        assertEquals("Asia/Kolkata", response.timeZoneId());
        assertEquals("DD_MM_YYYY", response.dateFormat());
        assertEquals("H24", response.timeFormat());
        assertEquals("SUNDAY", response.weekStart());
        assertEquals("DETAILED", response.aiResponseStyle());
        assertFalse(response.aiPersonalizationEnabled());
        verify(settings).save(argThat(saved -> saved.getUser() == user));
    }

    @Test
    void rejectsUnknownTimeZonesInsteadOfSavingThem() {
        when(users.findById(17L)).thenReturn(Optional.of(activeUser(17L)));

        assertThrows(BadRequestException.class, () -> service.updateForUser(17L,
                new UserSettingsRequest(true, true, true, true, "Not/A_Zone",
                        "LOCALE", "LOCALE", "MONDAY", "BALANCED", true)));

        verify(settings, never()).save(any(UserSettings.class));
    }

    @Test
    void notificationCategoriesRespectPreferencesAndSystemNotificationsRemainEnabled() {
        UserSettings preference = new UserSettings();
        preference.setTaskRemindersEnabled(false);
        preference.setHabitRemindersEnabled(false);
        when(settings.findByUser_Id(17L)).thenReturn(Optional.of(preference));

        assertFalse(service.notificationEnabled(17L, NotificationType.TASK_OVERDUE));
        assertFalse(service.notificationEnabled(17L, NotificationType.HABIT_COMPLETED));
        assertTrue(service.notificationEnabled(17L, NotificationType.SYSTEM));
    }

    private User activeUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setActive(true);
        return user;
    }
}

package com.lifesync.lifesync.user.service;

import com.lifesync.lifesync.notification.entity.NotificationType;
import com.lifesync.lifesync.user.dto.UserSettingsRequest;
import com.lifesync.lifesync.user.dto.UserSettingsResponse;

public interface UserSettingsService {
    UserSettingsResponse getForUser(Long userId);
    UserSettingsResponse updateForUser(Long userId, UserSettingsRequest request);
    boolean notificationEnabled(Long userId, NotificationType type);
}

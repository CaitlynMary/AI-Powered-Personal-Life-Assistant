package com.lifesync.lifesync.notification.service;
import com.lifesync.lifesync.notification.dto.NotificationPageResponse;
public interface NotificationService {
    NotificationPageResponse getAll(Long userId,int page,int size,boolean unreadOnly);
    void markRead(Long userId,Long id);
    int markAllRead(Long userId);
    long unreadCount(Long userId);
}

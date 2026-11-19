package com.lifesync.lifesync.reminder.service;
import com.lifesync.lifesync.reminder.dto.*;
public interface ReminderService {
    ReminderResponse create(Long userId,ReminderRequest request);
    ReminderPageResponse getAll(Long userId,int page,int size);
    ReminderResponse getById(Long userId,Long id);
    ReminderResponse update(Long userId,Long id,ReminderRequest request);
    void cancel(Long userId,Long id);
    ReminderPageResponse upcoming(Long userId,int page,int size);
    int processDueReminders();
}

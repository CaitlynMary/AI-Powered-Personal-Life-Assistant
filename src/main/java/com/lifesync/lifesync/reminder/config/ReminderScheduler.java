package com.lifesync.lifesync.reminder.config;

import com.lifesync.lifesync.reminder.service.ReminderService;
import com.lifesync.lifesync.notification.service.NotificationEventService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReminderScheduler {
    private final ReminderService reminderService;
    private final NotificationEventService notificationEvents;
    public ReminderScheduler(ReminderService reminderService,NotificationEventService notificationEvents){this.reminderService=reminderService;this.notificationEvents=notificationEvents;}
    @Scheduled(fixedDelayString="${app.reminders.poll-interval-ms:30000}")
    public void processDueReminders(){reminderService.processDueReminders();}
    @Scheduled(fixedDelayString="${app.reminders.poll-interval-ms:30000}")
    public void processTaskDeadlineNotifications(){notificationEvents.processTaskDeadlineNotifications();}
}

package com.lifesync.lifesync.notification.service;

import com.lifesync.lifesync.exception.ResourceNotFoundException;
import com.lifesync.lifesync.notification.entity.Notification;
import com.lifesync.lifesync.notification.entity.NotificationType;
import com.lifesync.lifesync.notification.repository.NotificationRepository;
import com.lifesync.lifesync.notification.service.impl.NotificationServiceImpl;
import com.lifesync.lifesync.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {
    @Mock NotificationRepository notifications;
    private NotificationServiceImpl service;
    private User owner;
    @BeforeEach void setUp(){service=new NotificationServiceImpl(notifications);owner=new User();owner.setId(11L);}

    @Test void listsAndCountsOnlyUserNotifications() {
        when(notifications.findAllByUser_IdOrderByCreatedAtDesc(eq(11L),any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(notification(1L,false))));
        when(notifications.findAllByUser_IdAndIsReadFalseOrderByCreatedAtDesc(eq(11L),any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(notification(1L,false))));
        when(notifications.countByUser_IdAndIsReadFalse(11L)).thenReturn(1L);
        assertEquals(1,service.getAll(11L,0,10,false).getContent().size());
        assertEquals(1,service.getAll(11L,0,10,true).getContent().size());
        assertEquals(1L,service.unreadCount(11L));
    }

    @Test void marksOwnedNotificationAndAllNotificationsRead() {
        Notification n=notification(3L,false);when(notifications.findByIdAndUser_Id(3L,11L)).thenReturn(Optional.of(n));
        when(notifications.save(n)).thenReturn(n);service.markRead(11L,3L);assertTrue(n.getIsRead());
        when(notifications.markAllRead(11L)).thenReturn(2);assertEquals(2,service.markAllRead(11L));
        when(notifications.findByIdAndUser_Id(99L,11L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->service.markRead(11L,99L));
    }

    private Notification notification(Long id,boolean read){Notification n=new Notification();n.setId(id);n.setUser(owner);n.setTitle("Reminder");n.setMessage("Due");n.setNotificationType(NotificationType.REMINDER);n.setIsRead(read);return n;}
}

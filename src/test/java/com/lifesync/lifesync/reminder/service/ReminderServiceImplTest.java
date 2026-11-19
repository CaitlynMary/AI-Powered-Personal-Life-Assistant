package com.lifesync.lifesync.reminder.service;

import com.lifesync.lifesync.exception.BadRequestException;
import com.lifesync.lifesync.exception.ResourceNotFoundException;
import com.lifesync.lifesync.notification.service.NotificationEventService;
import com.lifesync.lifesync.reminder.dto.ReminderRequest;
import com.lifesync.lifesync.reminder.entity.*;
import com.lifesync.lifesync.reminder.repository.ReminderRepository;
import com.lifesync.lifesync.reminder.service.impl.ReminderServiceImpl;
import com.lifesync.lifesync.task.entity.Task;
import com.lifesync.lifesync.task.entity.TaskStatus;
import com.lifesync.lifesync.task.repository.TaskRepository;
import com.lifesync.lifesync.user.entity.User;
import com.lifesync.lifesync.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReminderServiceImplTest {
    @Mock ReminderRepository reminders;
    @Mock NotificationEventService notificationEvents;
    @Mock UserRepository users;
    @Mock TaskRepository tasks;
    private ReminderServiceImpl service;
    private User owner;
    @BeforeEach void setUp(){service=new ReminderServiceImpl(reminders,notificationEvents,users,tasks);owner=new User();owner.setId(5L);}

    @Test void createsReminderForAuthenticatedOwner() {
        when(users.findById(5L)).thenReturn(Optional.of(owner));
        when(reminders.save(any(Reminder.class))).thenAnswer(i->{Reminder r=i.getArgument(0);r.setId(9L);return r;});
        var response=service.create(5L,request(LocalDateTime.now().plusHours(1)));
        ArgumentCaptor<Reminder> captor=ArgumentCaptor.forClass(Reminder.class);verify(reminders).save(captor.capture());
        assertEquals(5L,captor.getValue().getUser().getId());assertEquals(ReminderStatus.PENDING,captor.getValue().getStatus());assertEquals(9L,response.getId());
    }

    @Test void createsTaskLinkedReminderForOwnedTaskWithDeadline() {
        Task task=new Task();task.setId(33L);task.setUser(owner);task.setTitle("Submit assignment");
        task.setDueDate(LocalDateTime.now().plusDays(1));task.setStatus(TaskStatus.TODO);
        when(users.findById(5L)).thenReturn(Optional.of(owner));
        when(tasks.findByIdAndUser_Id(33L,5L)).thenReturn(Optional.of(task));
        when(reminders.save(any(Reminder.class))).thenAnswer(i->{Reminder r=i.getArgument(0);r.setId(14L);return r;});

        ReminderRequest request=new ReminderRequest(task.getTitle(),"Task reminder",
                task.getDueDate().minusHours(1),ReminderType.TASK,33L);
        request.setDeadlineOffsetMinutes(60);
        var response=service.create(5L,request);

        assertEquals(33L,response.getTaskId());
        assertEquals(60,response.getDeadlineOffsetMinutes());
        assertEquals(task.getDueDate().minusMinutes(60),response.getReminderDateTime());
        verify(tasks).findByIdAndUser_Id(33L,5L);
    }

    @Test void rejectsInvalidDeadlineOffsets() {
        ReminderRequest standalone=request(LocalDateTime.now().plusHours(1));
        standalone.setDeadlineOffsetMinutes(15);
        assertThrows(BadRequestException.class,()->service.create(5L,standalone));

        ReminderRequest excessive=new ReminderRequest("Reminder","Message",LocalDateTime.now().plusHours(1),
                ReminderType.TASK,33L);
        excessive.setDeadlineOffsetMinutes(525601);
        assertThrows(BadRequestException.class,()->service.create(5L,excessive));
    }

    @Test void rejectsTaskLinkedReminderForAnotherUsersTask() {
        when(users.findById(5L)).thenReturn(Optional.of(owner));
        when(tasks.findByIdAndUser_Id(33L,5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->service.create(5L,
                new ReminderRequest("Reminder","Message",LocalDateTime.now().plusHours(1),ReminderType.TASK,33L)));
        verify(reminders,never()).save(any(Reminder.class));
    }

    @Test void rejectsInvalidReminderFields() {
        assertThrows(BadRequestException.class,()->service.create(5L,new ReminderRequest(" ","msg",LocalDateTime.now(),ReminderType.GENERAL)));
        assertThrows(BadRequestException.class,()->service.create(5L,new ReminderRequest("title","msg",null,ReminderType.GENERAL)));
    }

    @Test void listsAndGetsOnlyOwnedReminders() {
        Reminder reminder=reminder(2L,ReminderStatus.PENDING);
        when(reminders.findAllByUser_Id(eq(5L),any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(reminder)));
        assertEquals(1,service.getAll(5L,0,10).getContent().size());
        when(reminders.findByIdAndUser_Id(2L,5L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->service.getById(5L,2L));
    }

    @Test void updatesPendingAndCancelsWithoutDeletingReminder() {
        Reminder reminder=reminder(2L,ReminderStatus.PENDING);when(reminders.findByIdAndUser_Id(2L,5L)).thenReturn(Optional.of(reminder));
        when(reminders.save(reminder)).thenReturn(reminder);
        assertEquals("Updated",service.update(5L,2L,new ReminderRequest("Updated","new message",LocalDateTime.now().plusDays(1),ReminderType.TASK)).getTitle());
        service.cancel(5L,2L);assertEquals(ReminderStatus.CANCELLED,reminder.getStatus());verify(reminders,never()).delete(any(Reminder.class));
        assertThrows(BadRequestException.class,()->service.update(5L,2L,request(LocalDateTime.now().plusDays(2))));
    }

    @Test void upcomingOnlyQueriesPendingRemindersForOwner() {
        when(reminders.findAllByUser_IdAndStatusAndReminderDateTimeGreaterThanEqual(eq(5L),eq(ReminderStatus.PENDING),any(),any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(reminder(4L,ReminderStatus.PENDING))));
        assertEquals(1,service.upcoming(5L,0,10).getContent().size());
        verify(reminders).findAllByUser_IdAndStatusAndReminderDateTimeGreaterThanEqual(eq(5L),eq(ReminderStatus.PENDING),any(),any(PageRequest.class));
    }

    @Test void dueReminderCreatesOneNotificationAndMarksSent() {
        Reminder reminder=reminder(7L,ReminderStatus.PENDING);
        when(reminders.findTop100ByStatusAndReminderDateTimeLessThanEqualOrderByReminderDateTimeAsc(eq(ReminderStatus.PENDING),any())).thenReturn(List.of(reminder));
        when(notificationEvents.reminderDue(reminder)).thenReturn(true);
        assertEquals(1,service.processDueReminders());
        verify(notificationEvents).reminderDue(reminder);
        assertEquals(ReminderStatus.SENT,reminder.getStatus());
        assertEquals(0,service.processDueReminders());
        verify(notificationEvents).reminderDue(reminder);
    }

    @Test void existingNotificationPreventsDuplicateAndReminderStillMovesToSent() {
        Reminder reminder=reminder(7L,ReminderStatus.PENDING);
        when(reminders.findTop100ByStatusAndReminderDateTimeLessThanEqualOrderByReminderDateTimeAsc(eq(ReminderStatus.PENDING),any())).thenReturn(List.of(reminder));
        when(notificationEvents.reminderDue(reminder)).thenReturn(false);
        assertEquals(0,service.processDueReminders());assertEquals(ReminderStatus.SENT,reminder.getStatus());
        verify(notificationEvents).reminderDue(reminder);
    }

    @Test void taskCompletedBeforeReminderFireCancelsUpcomingReminder() {
        Task task=new Task();task.setId(33L);task.setUser(owner);task.setTitle("Submit project report");task.setStatus(TaskStatus.COMPLETED);
        Reminder reminder=reminder(12L,ReminderStatus.PENDING);reminder.setTask(task);
        when(reminders.findTop100ByStatusAndReminderDateTimeLessThanEqualOrderByReminderDateTimeAsc(eq(ReminderStatus.PENDING),any())).thenReturn(List.of(reminder));
        assertEquals(0,service.processDueReminders());
        assertEquals(ReminderStatus.CANCELLED,reminder.getStatus());
        verify(notificationEvents,never()).reminderDue(any());
    }

    @Test void taskMissedBeforeReminderFireCancelsUpcomingReminder() {
        Task task=new Task();task.setId(33L);task.setUser(owner);task.setTitle("Submit project report");task.setStatus(TaskStatus.MISSED);
        Reminder reminder=reminder(12L,ReminderStatus.PENDING);reminder.setTask(task);
        when(reminders.findTop100ByStatusAndReminderDateTimeLessThanEqualOrderByReminderDateTimeAsc(eq(ReminderStatus.PENDING),any())).thenReturn(List.of(reminder));
        assertEquals(0,service.processDueReminders());
        assertEquals(ReminderStatus.CANCELLED,reminder.getStatus());
        verify(notificationEvents,never()).reminderDue(any());
    }

    private ReminderRequest request(LocalDateTime date){return new ReminderRequest("Reminder","Reminder message",date,ReminderType.GENERAL);}
    private Reminder reminder(Long id,ReminderStatus status){Reminder r=new Reminder();r.setId(id);r.setUser(owner);r.setTitle("Reminder");r.setMessage("Message");r.setReminderDateTime(LocalDateTime.now().minusMinutes(1));r.setReminderType(ReminderType.GENERAL);r.setStatus(status);return r;}
}

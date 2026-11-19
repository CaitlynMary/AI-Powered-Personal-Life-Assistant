package com.lifesync.lifesync.ai.service;

import com.lifesync.lifesync.calendar.service.CalendarEventService;
import com.lifesync.lifesync.goal.service.GoalService;
import com.lifesync.lifesync.habit.repository.HabitLogRepository;
import com.lifesync.lifesync.habit.service.HabitService;
import com.lifesync.lifesync.milestone.repository.GoalMilestoneRepository;
import com.lifesync.lifesync.note.dto.NotePageResponse;
import com.lifesync.lifesync.note.service.NoteService;
import com.lifesync.lifesync.notification.service.NotificationService;
import com.lifesync.lifesync.reminder.dto.ReminderPageResponse;
import com.lifesync.lifesync.reminder.service.ReminderService;
import com.lifesync.lifesync.schedule.repository.ScheduleSlotRepository;
import com.lifesync.lifesync.task.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class AiContextServiceTest {
    private TaskRepository tasks;
    private GoalService goals;
    private GoalMilestoneRepository milestones;
    private HabitService habits;
    private HabitLogRepository habitLogs;
    private CalendarEventService calendar;
    private ScheduleSlotRepository schedules;
    private ReminderService reminders;
    private NotificationService notifications;
    private NoteService notes;
    private AiContextService service;

    @BeforeEach
    void setUp() {
        tasks = mock(TaskRepository.class);
        goals = mock(GoalService.class);
        milestones = mock(GoalMilestoneRepository.class);
        habits = mock(HabitService.class);
        habitLogs = mock(HabitLogRepository.class);
        calendar = mock(CalendarEventService.class);
        schedules = mock(ScheduleSlotRepository.class);
        reminders = mock(ReminderService.class);
        notifications = mock(NotificationService.class);
        notes = mock(NoteService.class);
        service = new AiContextService(tasks, goals, milestones, habits, habitLogs, calendar,
                schedules, reminders, notifications, notes);
    }

    @Test
    void retrievesNotesOnlyWhenQuestionExplicitlyMentionsJournalingAndUsesAuthenticatedUserId() {
        when(notes.getAll(42L, 0, 5, "", "updatedAt", "DESC"))
                .thenReturn(new NotePageResponse(List.of(), 0, 5, 0, 0, true));

        String context = service.forQuestion(42L, "What did I write in my journal?");

        assertTrue(context.contains("RECENT NOTE CONTENT"));
        verify(notes).getAll(42L, 0, 5, "", "updatedAt", "DESC");
        verifyNoInteractions(tasks, goals, milestones, habits, habitLogs, calendar, schedules, reminders, notifications);
    }

    @Test
    void genericQuestionDoesNotLoadPersonalRecords() {
        assertEquals("", service.forQuestion(42L, "How can I make a simple morning routine?"));

        verifyNoInteractions(tasks, goals, milestones, habits, habitLogs, calendar, schedules,
                reminders, notifications, notes);
    }

    @Test
    void reminderQuestionDoesNotLoadUnrelatedPersonalModules() {
        when(reminders.upcoming(42L, 0, 10))
                .thenReturn(new ReminderPageResponse(List.of(), 0, 10, 0, 0, true));

        String context = service.forQuestion(42L, "What reminders are coming up?");

        assertTrue(context.contains("No upcoming pending reminders"));
        verify(reminders).upcoming(42L, 0, 10);
        verifyNoInteractions(tasks, goals, milestones, habits, habitLogs, calendar, schedules,
                notifications, notes);
    }
}

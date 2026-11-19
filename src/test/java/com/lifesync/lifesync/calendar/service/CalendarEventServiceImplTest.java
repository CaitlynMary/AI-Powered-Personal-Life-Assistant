package com.lifesync.lifesync.calendar.service;

import com.lifesync.lifesync.calendar.dto.CalendarEventRequest;
import com.lifesync.lifesync.calendar.dto.CalendarEventResponse;
import com.lifesync.lifesync.calendar.entity.CalendarEvent;
import com.lifesync.lifesync.calendar.entity.CalendarEventType;
import com.lifesync.lifesync.calendar.entity.CalendarRecurrenceType;
import com.lifesync.lifesync.calendar.repository.CalendarEventRepository;
import com.lifesync.lifesync.calendar.service.impl.CalendarEventServiceImpl;
import com.lifesync.lifesync.exception.BadRequestException;
import com.lifesync.lifesync.exception.DuplicateResourceException;
import com.lifesync.lifesync.exception.ResourceNotFoundException;
import com.lifesync.lifesync.task.repository.TaskRepository;
import com.lifesync.lifesync.user.entity.User;
import com.lifesync.lifesync.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CalendarEventServiceImplTest {
    private CalendarEventRepository eventRepository;
    private UserRepository userRepository;
    private TaskRepository taskRepository;
    private CalendarEventServiceImpl service;
    private final LocalDateTime base = LocalDateTime.of(2026, 11, 4, 0, 0);

    @BeforeEach
    void setUp() {
        eventRepository = mock(CalendarEventRepository.class);
        userRepository = mock(UserRepository.class);
        taskRepository = mock(TaskRepository.class);
        service = new CalendarEventServiceImpl(eventRepository, userRepository, taskRepository);
        when(userRepository.findById(7L)).thenReturn(Optional.of(new User()));
        when(eventRepository.findCandidatesForRange(anyLong(), any(), any(), any(), any())).thenReturn(List.of());
        when(eventRepository.save(any(CalendarEvent.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void createsAnOwnedCalendarEvent() {
        CalendarEventResponse response = service.create(7L, request(base.plusHours(9), base.plusHours(10), CalendarRecurrenceType.NONE));
        assertEquals("Planning", response.getTitle());
        assertEquals(CalendarEventType.WORK, response.getEventType());
        verify(userRepository).findById(7L);
        verify(eventRepository).save(any(CalendarEvent.class));
    }

    @Test
    void createsAllDayEvents() {
        CalendarEventRequest request = request(base, base.plusDays(1), CalendarRecurrenceType.NONE);
        request.setAllDay(true);
        assertTrue(service.create(7L, request).getAllDay());
    }

    @Test
    void rejectsInvalidTimeRange() {
        assertThrows(BadRequestException.class, () -> service.create(7L, request(base.plusHours(10), base.plusHours(9), CalendarRecurrenceType.NONE)));
        verify(eventRepository, never()).save(any());
    }

    @Test
    void rejectsOverlapWithAnExistingEvent() {
        CalendarEvent existing = event(11L, base.plusHours(9), base.plusHours(11), CalendarRecurrenceType.NONE);
        when(eventRepository.findCandidatesForRange(7L, null, base.plusHours(10), base.plusHours(12), CalendarRecurrenceType.NONE))
                .thenReturn(List.of(existing));
        assertThrows(DuplicateResourceException.class,
                () -> service.create(7L, request(base.plusHours(10), base.plusHours(12), CalendarRecurrenceType.NONE)));
    }

    @Test
    void allowsAdjacentEvents() {
        CalendarEventResponse response = service.create(7L, request(base.plusHours(10), base.plusHours(11), CalendarRecurrenceType.NONE));
        assertEquals(base.plusHours(10), response.getStartDateTime());
    }

    @Test
    void getsAndUpdatesOnlyAnOwnedEvent() {
        CalendarEvent existing = event(12L, base.plusHours(9), base.plusHours(10), CalendarRecurrenceType.NONE);
        when(eventRepository.findOwnedById(12L, 7L)).thenReturn(Optional.of(existing));
        assertEquals(12L, service.getById(7L, 12L).getId());
        CalendarEventResponse updated = service.update(7L, 12L, request(base.plusHours(11), base.plusHours(12), CalendarRecurrenceType.NONE));
        assertEquals(base.plusHours(11), updated.getStartDateTime());
    }

    @Test
    void hidesOtherUsersEvents() {
        when(eventRepository.findOwnedById(99L, 7L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getById(7L, 99L));
        verify(eventRepository, never()).delete(any());
    }

    @Test
    void deletesAnOwnedEvent() {
        CalendarEvent existing = event(13L, base.plusHours(9), base.plusHours(10), CalendarRecurrenceType.NONE);
        when(eventRepository.findOwnedById(13L, 7L)).thenReturn(Optional.of(existing));
        service.delete(7L, 13L);
        verify(eventRepository).delete(existing);
    }

    @Test
    void dailyViewExpandsDailyRecurringEvent() {
        CalendarEvent recurring = event(20L, base.plusHours(9), base.plusHours(10), CalendarRecurrenceType.DAILY);
        when(eventRepository.findCandidatesForRange(7L, null, base.plusDays(2), base.plusDays(3), CalendarRecurrenceType.NONE))
                .thenReturn(List.of(recurring));
        List<CalendarEventResponse> results = service.day(7L, LocalDate.of(2026, 11, 6));
        assertEquals(1, results.size());
        assertEquals(base.plusDays(2).plusHours(9), results.get(0).getStartDateTime());
        assertEquals(20L, results.get(0).getId());
    }

    @Test
    void weeklyViewUsesSevenDayWindowAndExpandsWeeklyEvents() {
        CalendarEvent recurring = event(22L, base.plusDays(1).plusHours(9), base.plusDays(1).plusHours(10), CalendarRecurrenceType.WEEKLY);
        LocalDateTime start = base;
        when(eventRepository.findCandidatesForRange(7L, null, start, start.plusDays(7), CalendarRecurrenceType.NONE))
                .thenReturn(List.of(recurring));
        List<CalendarEventResponse> results = service.week(7L, LocalDate.of(2026, 11, 4));
        assertEquals(1, results.size());
        assertEquals(base.plusDays(1).plusHours(9), results.get(0).getStartDateTime());
    }

    @Test
    void monthlyViewExpandsMonthlyEventFromEndOfMonth() {
        CalendarEvent recurring = event(21L, LocalDateTime.of(2026, 8, 31, 14, 0), LocalDateTime.of(2026, 8, 31, 15, 0), CalendarRecurrenceType.MONTHLY);
        LocalDateTime september = LocalDateTime.of(2026, 9, 1, 0, 0);
        when(eventRepository.findCandidatesForRange(7L, null, september, september.plusMonths(1), CalendarRecurrenceType.NONE))
                .thenReturn(List.of(recurring));
        List<CalendarEventResponse> results = service.month(7L, 2026, 9);
        assertEquals(1, results.size());
        assertEquals(LocalDateTime.of(2026, 9, 30, 14, 0), results.get(0).getStartDateTime());
    }

    @Test
    void filtersEventsByTypeAndDateRange() {
        LocalDateTime start = base;
        LocalDateTime end = base.plusDays(1);
        service.getEvents(7L, CalendarEventType.MEETING, start, end);
        verify(eventRepository).findCandidatesForRange(7L, CalendarEventType.MEETING, start, end, CalendarRecurrenceType.NONE);
    }

    @Test
    void rejectsIncompleteDateRangeAndInvalidMonth() {
        assertThrows(BadRequestException.class, () -> service.getEvents(7L, null, base, null));
        assertThrows(BadRequestException.class, () -> service.month(7L, 2026, 13));
    }

    private CalendarEventRequest request(LocalDateTime start, LocalDateTime end, CalendarRecurrenceType recurrence) {
        return new CalendarEventRequest("Planning", "Plan project", CalendarEventType.WORK, false, start, end, recurrence, null);
    }

    private CalendarEvent event(Long id, LocalDateTime start, LocalDateTime end, CalendarRecurrenceType recurrence) {
        CalendarEvent event = new CalendarEvent();
        event.setId(id); event.setTitle("Existing"); event.setDescription("Existing event");
        event.setEventType(CalendarEventType.MEETING); event.setAllDay(false);
        event.setStartDateTime(start); event.setEndDateTime(end); event.setRecurrenceType(recurrence);
        return event;
    }
}

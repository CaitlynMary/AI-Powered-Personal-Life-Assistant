package com.lifesync.lifesync.schedule.service;

import com.lifesync.lifesync.exception.BadRequestException;
import com.lifesync.lifesync.exception.DuplicateResourceException;
import com.lifesync.lifesync.exception.ResourceNotFoundException;
import com.lifesync.lifesync.calendar.dto.CalendarEventResponse;
import com.lifesync.lifesync.calendar.service.CalendarEventService;
import com.lifesync.lifesync.schedule.dto.ScheduleConflictRequest;
import com.lifesync.lifesync.schedule.dto.ScheduleConflictResponse;
import com.lifesync.lifesync.schedule.dto.DailyScheduleApprovalItem;
import com.lifesync.lifesync.schedule.dto.DailyScheduleApprovalRequest;
import com.lifesync.lifesync.schedule.dto.ScheduleSlotResponse;
import com.lifesync.lifesync.schedule.dto.ScheduleSlotRequest;
import com.lifesync.lifesync.schedule.entity.ScheduleSlot;
import com.lifesync.lifesync.schedule.repository.ScheduleSlotRepository;
import com.lifesync.lifesync.schedule.service.impl.ScheduleServiceImpl;
import com.lifesync.lifesync.task.entity.Task;
import com.lifesync.lifesync.task.entity.TaskStatus;
import com.lifesync.lifesync.task.repository.TaskRepository;
import com.lifesync.lifesync.user.entity.User;
import com.lifesync.lifesync.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ScheduleServiceImplTest {
    private ScheduleSlotRepository scheduleRepository;
    private TaskRepository taskRepository;
    private CalendarEventService calendarEventService;
    private UserRepository userRepository;
    private ScheduleServiceImpl service;
    private final LocalDateTime day = LocalDate.of(2026, 11, 4).atStartOfDay();

    @BeforeEach
    void setUp() {
        scheduleRepository = mock(ScheduleSlotRepository.class);
        taskRepository = mock(TaskRepository.class);
        calendarEventService = mock(CalendarEventService.class);
        userRepository = mock(UserRepository.class);
        service = new ScheduleServiceImpl(scheduleRepository, calendarEventService, taskRepository, userRepository);
        when(userRepository.findById(7L)).thenReturn(Optional.of(new User()));
        when(scheduleRepository.findOverlappingRange(anyLong(), any(), any())).thenReturn(List.of());
        when(taskRepository.findAllByUser_IdAndStartDateLessThanAndDueDateGreaterThan(anyLong(), any(), any())).thenReturn(List.of());
        when(calendarEventService.getEvents(anyLong(), isNull(), any(), any())).thenReturn(List.of());
        when(scheduleRepository.save(any(ScheduleSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsSlotForAuthenticatedOwner() {
        ScheduleSlotRequest request = request(day.plusHours(9), day.plusHours(10));
        assertEquals("Study", service.create(7L, request).getTitle());
        verify(userRepository).findById(7L);
        verify(scheduleRepository).save(any(ScheduleSlot.class));
    }

    @Test
    void allowsAdjacentSlots() {
        when(scheduleRepository.findOverlappingRange(7L, day.plusHours(9), day.plusHours(10))).thenReturn(List.of());
        assertDoesNotThrow(() -> service.create(7L, request(day.plusHours(9), day.plusHours(10))));
    }

    @Test
    void rejectsOverlappingSlotWhenCreating() {
        when(scheduleRepository.findOverlappingRange(7L, day.plusHours(9), day.plusHours(11)))
                .thenReturn(List.of(slot(2L, day.plusHours(10), day.plusHours(12))));
        assertThrows(DuplicateResourceException.class, () -> service.create(7L, request(day.plusHours(9), day.plusHours(11))));
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void preventsCreatingASecondScheduleForTheSameTask() {
        when(taskRepository.findByIdAndUser_Id(10L, 7L)).thenReturn(Optional.of(pendingTask(10L)));
        when(scheduleRepository.findAllByUser_IdAndTask_IdOrderByStartDateTimeAsc(7L, 10L))
                .thenReturn(List.of(slot(3L, day.plusHours(8), day.plusHours(9))));

        assertThrows(DuplicateResourceException.class,
                () -> service.create(7L, request(day.plusHours(10), day.plusHours(11), 10L)));
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void approvesDailyPlanByCreatingOrUpdatingTheExistingTaskSchedule() {
        Task task = pendingTask(10L);
        when(taskRepository.findByIdAndUser_Id(10L, 7L)).thenReturn(Optional.of(task));
        ScheduleSlot existing = slot(3L, day.plusHours(8), day.plusHours(9));
        existing.setTask(task);
        when(scheduleRepository.findAllByUser_IdAndTask_IdOrderByStartDateTimeAsc(7L, 10L))
                .thenReturn(List.of(existing));
        LocalDate date = LocalDate.now().plusDays(1);

        List<ScheduleSlotResponse> result = service.approveDailyPlan(7L,
                new DailyScheduleApprovalRequest(date, List.of(
                        new DailyScheduleApprovalItem(10L, LocalTime.of(10, 0), LocalTime.of(11, 0)))));

        assertEquals(1, result.size());
        assertEquals(task.getTitle(), existing.getTitle());
        assertEquals(date.atTime(10, 0), existing.getStartDateTime());
        assertEquals(task, existing.getTask());
        verify(scheduleRepository).save(existing);
    }

    @Test
    void rejectsDuplicateTaskSuggestionsWithoutSaving() {
        LocalDate date = LocalDate.now().plusDays(1);
        DailyScheduleApprovalRequest request = new DailyScheduleApprovalRequest(date, List.of(
                new DailyScheduleApprovalItem(10L, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                new DailyScheduleApprovalItem(10L, LocalTime.of(10, 0), LocalTime.of(11, 0))));

        assertThrows(BadRequestException.class, () -> service.approveDailyPlan(7L, request));
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void conflictEndpointReturnsOverlappingSlotDetails() {
        when(scheduleRepository.findOverlappingRange(7L, day.plusHours(9), day.plusHours(11)))
                .thenReturn(List.of(slot(2L, day.plusHours(10), day.plusHours(12))));
        ScheduleConflictResponse response = service.detectConflicts(7L, new ScheduleConflictRequest(day.plusHours(9), day.plusHours(11), null));
        assertTrue(response.isHasConflicts());
        assertEquals("SCHEDULE_SLOT", response.getConflicts().get(0).getType());
        assertEquals(2L, response.getConflicts().get(0).getId());
    }

    @Test
    void rejectsInvalidTimeRange() {
        assertThrows(BadRequestException.class, () -> service.create(7L, request(day.plusHours(10), day.plusHours(10))));
    }

    @Test
    void dailyAndWeeklyQueriesUseDateWindow() {
        service.daily(7L, LocalDate.of(2026, 11, 4));
        verify(scheduleRepository).findOverlappingRange(7L, day, day.plusDays(1));
        service.weekly(7L, LocalDate.of(2026, 11, 4));
        verify(scheduleRepository).findOverlappingRange(7L, day, day.plusDays(7));
    }

    @Test
    void taskLinkMustBeOwnedByCurrentUser() {
        when(taskRepository.findByIdAndUser_Id(99L, 7L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.create(7L, request(day.plusHours(9), day.plusHours(10), 99L)));
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void retrievesAndUpdatesOnlyOwnedSlot() {
        ScheduleSlot existing = slot(4L, day.plusHours(8), day.plusHours(9));
        when(scheduleRepository.findByIdAndUser_Id(4L, 7L)).thenReturn(Optional.of(existing));
        assertEquals(4L, service.getById(7L, 4L).getId());
        when(scheduleRepository.findOverlappingRangeExcludingId(7L, day.plusHours(10), day.plusHours(11), 4L)).thenReturn(List.of());
        assertEquals(day.plusHours(10), service.update(7L, 4L, request(day.plusHours(10), day.plusHours(11))).getStartDateTime());
        verify(scheduleRepository).save(existing);
    }

    @Test
    void hidesAnotherUsersSlotAsNotFound() {
        when(scheduleRepository.findByIdAndUser_Id(4L, 7L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getById(7L, 4L));
        verify(scheduleRepository, never()).delete(any());
    }

    @Test
    void deletesOwnedSlot() {
        ScheduleSlot existing = slot(4L, day.plusHours(8), day.plusHours(9));
        when(scheduleRepository.findByIdAndUser_Id(4L, 7L)).thenReturn(Optional.of(existing));
        assertDoesNotThrow(() -> service.delete(7L, 4L));
        verify(scheduleRepository).delete(existing);
    }

    @Test
    void taskIntervalsAreReportedAsConflicts() {
        Task task = new Task();
        task.setId(3L); task.setTitle("Existing task"); task.setStartDate(day.plusHours(10)); task.setDueDate(day.plusHours(12));
        when(taskRepository.findAllByUser_IdAndStartDateLessThanAndDueDateGreaterThan(7L, day.plusHours(11), day.plusHours(9))).thenReturn(List.of(task));
        ScheduleConflictResponse response = service.detectConflicts(7L, new ScheduleConflictRequest(day.plusHours(9), day.plusHours(11), null));
        assertTrue(response.isHasConflicts());
        assertEquals("TASK", response.getConflicts().get(0).getType());
    }

    @Test
    void calendarEventsAreReportedAsConflicts() {
        CalendarEventResponse event = new CalendarEventResponse();
        event.setId(8L); event.setTitle("Team meeting");
        event.setStartDateTime(day.plusHours(10)); event.setEndDateTime(day.plusHours(12));
        when(calendarEventService.getEvents(7L, null, day.plusHours(9), day.plusHours(11))).thenReturn(List.of(event));
        ScheduleConflictResponse response = service.detectConflicts(7L,
                new ScheduleConflictRequest(day.plusHours(9), day.plusHours(11), null));
        assertTrue(response.isHasConflicts());
        assertEquals("CALENDAR_EVENT", response.getConflicts().get(0).getType());
        assertEquals(8L, response.getConflicts().get(0).getId());
    }

    private ScheduleSlotRequest request(LocalDateTime start, LocalDateTime end) { return request(start, end, null); }
    private ScheduleSlotRequest request(LocalDateTime start, LocalDateTime end, Long taskId) {
        return new ScheduleSlotRequest("Study", "Session", start, end, taskId);
    }
    private ScheduleSlot slot(Long id, LocalDateTime start, LocalDateTime end) {
        ScheduleSlot slot = new ScheduleSlot(); slot.setId(id); slot.setTitle("Existing slot"); slot.setStartDateTime(start); slot.setEndDateTime(end); return slot;
    }
    private Task pendingTask(Long id) {
        Task task = new Task();
        task.setId(id);
        task.setTitle("Planned task");
        task.setStatus(TaskStatus.TODO);
        return task;
    }
}

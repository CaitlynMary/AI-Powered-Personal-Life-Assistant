package com.lifesync.lifesync.calendar.service.impl;

import com.lifesync.lifesync.calendar.dto.CalendarEventRequest;
import com.lifesync.lifesync.calendar.dto.CalendarEventResponse;
import com.lifesync.lifesync.calendar.entity.CalendarEvent;
import com.lifesync.lifesync.calendar.entity.CalendarEventType;
import com.lifesync.lifesync.calendar.entity.CalendarRecurrenceType;
import com.lifesync.lifesync.calendar.repository.CalendarEventRepository;
import com.lifesync.lifesync.calendar.service.CalendarEventService;
import com.lifesync.lifesync.exception.BadRequestException;
import com.lifesync.lifesync.exception.DuplicateResourceException;
import com.lifesync.lifesync.exception.ResourceNotFoundException;
import com.lifesync.lifesync.task.entity.Task;
import com.lifesync.lifesync.task.repository.TaskRepository;
import com.lifesync.lifesync.user.entity.User;
import com.lifesync.lifesync.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class CalendarEventServiceImpl implements CalendarEventService {
    // Recurrence has no end-date field in this module; conflict checking covers the next year.
    private static final long RECURRING_CONFLICT_HORIZON_YEARS = 1;
    private final CalendarEventRepository eventRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public CalendarEventServiceImpl(CalendarEventRepository eventRepository, UserRepository userRepository, TaskRepository taskRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    @Override @Transactional
    public CalendarEventResponse create(Long userId, CalendarEventRequest request) {
        validate(request);
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User account was not found"));
        Task task = resolveTask(userId, request.getTaskId());
        CalendarEvent event = new CalendarEvent();
        event.setUser(user);
        apply(event, request, task);
        assertNoConflicts(userId, event, null);
        return response(eventRepository.save(event), event.getStartDateTime(), event.getEndDateTime());
    }

    @Override @Transactional(readOnly = true)
    public List<CalendarEventResponse> getEvents(Long userId, CalendarEventType eventType, LocalDateTime start, LocalDateTime end) {
        if ((start == null) != (end == null)) throw new BadRequestException("Both startDateTime and endDateTime are required for a date range");
        if (start == null) {
            return eventRepository.findAllForUser(userId, eventType).stream()
                    .map(event -> response(event, event.getStartDateTime(), event.getEndDateTime())).toList();
        }
        validateRange(start, end);
        return expandCandidates(userId, eventType, start, end);
    }

    @Override @Transactional(readOnly = true)
    public CalendarEventResponse getById(Long userId, Long id) {
        CalendarEvent event = findOwnedEvent(userId, id);
        return response(event, event.getStartDateTime(), event.getEndDateTime());
    }

    @Override @Transactional
    public CalendarEventResponse update(Long userId, Long id, CalendarEventRequest request) {
        validate(request);
        CalendarEvent event = findOwnedEvent(userId, id);
        Task task = resolveTask(userId, request.getTaskId());
        apply(event, request, task);
        assertNoConflicts(userId, event, id);
        return response(eventRepository.save(event), event.getStartDateTime(), event.getEndDateTime());
    }

    @Override @Transactional
    public void delete(Long userId, Long id) { eventRepository.delete(findOwnedEvent(userId, id)); }

    @Override @Transactional(readOnly = true)
    public List<CalendarEventResponse> day(Long userId, LocalDate date) {
        if (date == null) throw new BadRequestException("date is required");
        LocalDateTime start = date.atStartOfDay();
        return expandCandidates(userId, null, start, start.plusDays(1));
    }

    @Override @Transactional(readOnly = true)
    public List<CalendarEventResponse> week(Long userId, LocalDate startDate) {
        if (startDate == null) throw new BadRequestException("startDate is required");
        LocalDateTime start = startDate.atStartOfDay();
        return expandCandidates(userId, null, start, start.plusDays(7));
    }

    @Override @Transactional(readOnly = true)
    public List<CalendarEventResponse> month(Long userId, int year, int month) {
        try {
            LocalDate firstDay = YearMonth.of(year, month).atDay(1);
            LocalDateTime start = firstDay.atStartOfDay();
            return expandCandidates(userId, null, start, start.plusMonths(1));
        } catch (DateTimeException ex) {
            throw new BadRequestException("year and month must describe a valid calendar month");
        }
    }

    private List<CalendarEventResponse> expandCandidates(Long userId, CalendarEventType type, LocalDateTime start, LocalDateTime end) {
        List<CalendarEventResponse> results = new ArrayList<>();
        List<CalendarEvent> candidates = eventRepository.findCandidatesForRange(userId, type, start, end, CalendarRecurrenceType.NONE);
        for (CalendarEvent event : candidates) results.addAll(expand(event, start, end));
        results.sort((left, right) -> left.getStartDateTime().compareTo(right.getStartDateTime()));
        return results;
    }

    private List<CalendarEventResponse> expand(CalendarEvent event, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        List<CalendarEventResponse> results = new ArrayList<>();
        Duration duration = Duration.between(event.getStartDateTime(), event.getEndDateTime());
        if (event.getRecurrenceType() == CalendarRecurrenceType.NONE) {
            if (overlaps(event.getStartDateTime(), event.getEndDateTime(), rangeStart, rangeEnd)) {
                results.add(response(event, event.getStartDateTime(), event.getEndDateTime()));
            }
            return results;
        }

        long occurrence = initialOccurrenceIndex(event, rangeStart);
        while (true) {
            LocalDateTime occurrenceStart = occurrenceStart(event, occurrence);
            if (!occurrenceStart.isBefore(rangeEnd)) break;
            LocalDateTime occurrenceEnd = occurrenceStart.plus(duration);
            if (overlaps(occurrenceStart, occurrenceEnd, rangeStart, rangeEnd)) {
                results.add(response(event, occurrenceStart, occurrenceEnd));
            }
            occurrence++;
        }
        return results;
    }

    private long initialOccurrenceIndex(CalendarEvent event, LocalDateTime rangeStart) {
        LocalDateTime base = event.getStartDateTime();
        if (!base.isBefore(rangeStart)) return 0;
        long periods = switch (event.getRecurrenceType()) {
            case DAILY -> ChronoUnit.DAYS.between(base.toLocalDate(), rangeStart.toLocalDate());
            case WEEKLY -> ChronoUnit.DAYS.between(base.toLocalDate(), rangeStart.toLocalDate()) / 7;
            case MONTHLY -> ChronoUnit.MONTHS.between(YearMonth.from(base), YearMonth.from(rangeStart));
            case NONE -> 0;
        };
        return Math.max(0, periods - 1);
    }

    private LocalDateTime occurrenceStart(CalendarEvent event, long occurrence) {
        return switch (event.getRecurrenceType()) {
            case DAILY -> event.getStartDateTime().plusDays(occurrence);
            case WEEKLY -> event.getStartDateTime().plusWeeks(occurrence);
            case MONTHLY -> event.getStartDateTime().plusMonths(occurrence);
            case NONE -> event.getStartDateTime();
        };
    }

    private void assertNoConflicts(Long userId, CalendarEvent proposed, Long excludedId) {
        LocalDateTime conflictWindowEnd = proposed.getRecurrenceType() == CalendarRecurrenceType.NONE
                ? proposed.getEndDateTime() : proposed.getStartDateTime().plusYears(RECURRING_CONFLICT_HORIZON_YEARS);
        List<CalendarEvent> existing = eventRepository.findCandidatesForRange(userId, null,
                proposed.getStartDateTime(), conflictWindowEnd, CalendarRecurrenceType.NONE);
        List<CalendarEventResponse> proposedOccurrences = expand(proposed, proposed.getStartDateTime(), conflictWindowEnd);
        for (CalendarEvent event : existing) {
            if (excludedId != null && excludedId.equals(event.getId())) continue;
            for (CalendarEventResponse existingOccurrence : expand(event, proposed.getStartDateTime(), conflictWindowEnd)) {
                for (CalendarEventResponse proposedOccurrence : proposedOccurrences) {
                    if (overlaps(proposedOccurrence.getStartDateTime(), proposedOccurrence.getEndDateTime(),
                            existingOccurrence.getStartDateTime(), existingOccurrence.getEndDateTime())) {
                        throw new DuplicateResourceException("The calendar event conflicts with an existing event");
                    }
                }
            }
        }
    }

    private boolean overlaps(LocalDateTime firstStart, LocalDateTime firstEnd, LocalDateTime secondStart, LocalDateTime secondEnd) {
        return firstStart.isBefore(secondEnd) && firstEnd.isAfter(secondStart);
    }

    private void validate(CalendarEventRequest request) {
        if (request.getStartDateTime() == null || request.getEndDateTime() == null) throw new BadRequestException("Event start and end date-times are required");
        if (!request.getStartDateTime().isBefore(request.getEndDateTime())) throw new BadRequestException("startDateTime must be before endDateTime");
        if (request.getEventType() == null) throw new BadRequestException("eventType is required");
        if (request.getAllDay() == null) throw new BadRequestException("allDay is required");
        if (request.getRecurrenceType() == null) throw new BadRequestException("recurrenceType is required");
    }

    private void validateRange(LocalDateTime start, LocalDateTime end) {
        if (!start.isBefore(end)) throw new BadRequestException("startDateTime must be before endDateTime");
    }

    private Task resolveTask(Long userId, Long taskId) {
        if (taskId == null) return null;
        return taskRepository.findByIdAndUser_Id(taskId, userId).orElseThrow(() -> new ResourceNotFoundException("Task was not found"));
    }

    private CalendarEvent findOwnedEvent(Long userId, Long id) {
        return eventRepository.findOwnedById(id, userId).orElseThrow(() -> new ResourceNotFoundException("Calendar event was not found"));
    }

    private void apply(CalendarEvent event, CalendarEventRequest request, Task task) {
        event.setTitle(request.getTitle().trim());
        event.setDescription(request.getDescription() == null || request.getDescription().isBlank() ? null : request.getDescription().trim());
        event.setEventType(request.getEventType());
        event.setAllDay(request.getAllDay());
        event.setStartDateTime(request.getStartDateTime());
        event.setEndDateTime(request.getEndDateTime());
        event.setRecurrenceType(request.getRecurrenceType());
        event.setTask(task);
    }

    private CalendarEventResponse response(CalendarEvent event, LocalDateTime start, LocalDateTime end) {
        Task task = event.getTask();
        return new CalendarEventResponse(event.getId(), event.getTitle(), event.getDescription(), event.getEventType(), event.getAllDay(),
                start, end, event.getRecurrenceType(), task == null ? null : task.getId(), task == null ? null : task.getTitle(),
                event.getCreatedAt(), event.getUpdatedAt());
    }
}

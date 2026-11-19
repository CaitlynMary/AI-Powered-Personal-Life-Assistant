package com.lifesync.lifesync.calendar.controller;

import com.lifesync.lifesync.calendar.dto.CalendarEventRequest;
import com.lifesync.lifesync.calendar.dto.CalendarEventResponse;
import com.lifesync.lifesync.calendar.entity.CalendarEventType;
import com.lifesync.lifesync.calendar.service.CalendarEventService;
import com.lifesync.lifesync.exception.BadRequestException;
import com.lifesync.lifesync.security.principal.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/calendar/events")
public class CalendarEventController {
    private final CalendarEventService calendarEventService;

    public CalendarEventController(CalendarEventService calendarEventService) { this.calendarEventService = calendarEventService; }

    @PostMapping
    public ResponseEntity<CalendarEventResponse> create(@AuthenticationPrincipal CustomUserDetails principal,
                                                         @Valid @RequestBody CalendarEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(calendarEventService.create(principal.getId(), request));
    }

    @GetMapping
    public List<CalendarEventResponse> getEvents(@AuthenticationPrincipal CustomUserDetails principal,
                                                  @RequestParam(required = false) String eventType,
                                                  @RequestParam(required = false) LocalDateTime startDateTime,
                                                  @RequestParam(required = false) LocalDateTime endDateTime) {
        return calendarEventService.getEvents(principal.getId(), parseType(eventType), startDateTime, endDateTime);
    }

    @GetMapping("/day")
    public List<CalendarEventResponse> day(@AuthenticationPrincipal CustomUserDetails principal, @RequestParam LocalDate date) {
        return calendarEventService.day(principal.getId(), date);
    }

    @GetMapping("/week")
    public List<CalendarEventResponse> week(@AuthenticationPrincipal CustomUserDetails principal, @RequestParam LocalDate startDate) {
        return calendarEventService.week(principal.getId(), startDate);
    }

    @GetMapping("/month")
    public List<CalendarEventResponse> month(@AuthenticationPrincipal CustomUserDetails principal,
                                              @RequestParam int year, @RequestParam int month) {
        return calendarEventService.month(principal.getId(), year, month);
    }

    @GetMapping("/{id}")
    public CalendarEventResponse getById(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        return calendarEventService.getById(principal.getId(), id);
    }

    @PutMapping("/{id}")
    public CalendarEventResponse update(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id,
                                        @Valid @RequestBody CalendarEventRequest request) {
        return calendarEventService.update(principal.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        calendarEventService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }

    private CalendarEventType parseType(String eventType) {
        if (eventType == null || eventType.isBlank()) return null;
        try { return CalendarEventType.valueOf(eventType.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { throw new BadRequestException("eventType must be PERSONAL, WORK, MEETING, TASK, or OTHER"); }
    }
}

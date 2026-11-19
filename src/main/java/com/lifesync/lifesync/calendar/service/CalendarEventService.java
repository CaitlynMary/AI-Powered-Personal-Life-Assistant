package com.lifesync.lifesync.calendar.service;

import com.lifesync.lifesync.calendar.dto.CalendarEventRequest;
import com.lifesync.lifesync.calendar.dto.CalendarEventResponse;
import com.lifesync.lifesync.calendar.entity.CalendarEventType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface CalendarEventService {
    CalendarEventResponse create(Long userId, CalendarEventRequest request);
    List<CalendarEventResponse> getEvents(Long userId, CalendarEventType eventType, LocalDateTime start, LocalDateTime end);
    CalendarEventResponse getById(Long userId, Long id);
    CalendarEventResponse update(Long userId, Long id, CalendarEventRequest request);
    void delete(Long userId, Long id);
    List<CalendarEventResponse> day(Long userId, LocalDate date);
    List<CalendarEventResponse> week(Long userId, LocalDate startDate);
    List<CalendarEventResponse> month(Long userId, int year, int month);
}

package com.lifesync.lifesync.schedule.service.impl;

import com.lifesync.lifesync.common.ApiConstants;
import com.lifesync.lifesync.calendar.dto.CalendarEventResponse;
import com.lifesync.lifesync.calendar.service.CalendarEventService;
import com.lifesync.lifesync.exception.BadRequestException;
import com.lifesync.lifesync.exception.DuplicateResourceException;
import com.lifesync.lifesync.exception.ResourceNotFoundException;
import com.lifesync.lifesync.schedule.dto.*;
import com.lifesync.lifesync.schedule.entity.ScheduleSlot;
import com.lifesync.lifesync.schedule.repository.ScheduleSlotRepository;
import com.lifesync.lifesync.schedule.service.ScheduleService;
import com.lifesync.lifesync.task.entity.Task;
import com.lifesync.lifesync.task.repository.TaskRepository;
import com.lifesync.lifesync.user.entity.User;
import com.lifesync.lifesync.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ScheduleServiceImpl implements ScheduleService {
    private final ScheduleSlotRepository scheduleRepository;
    private final CalendarEventService calendarEventService;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public ScheduleServiceImpl(ScheduleSlotRepository scheduleRepository, CalendarEventService calendarEventService,
                               TaskRepository taskRepository, UserRepository userRepository) {
        this.scheduleRepository = scheduleRepository;
        this.calendarEventService = calendarEventService;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Override @Transactional
    public ScheduleSlotResponse create(Long userId, ScheduleSlotRequest request) {
        validateInterval(request.getStartDateTime(), request.getEndDateTime());
        Task linkedTask = resolveTask(userId, request.getTaskId());
        ensureTaskNotAlreadyScheduled(userId, request.getTaskId(), null);
        ensureNoConflicts(userId, request.getStartDateTime(), request.getEndDateTime(), null, request.getTaskId());
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User account was not found"));
        ScheduleSlot slot = new ScheduleSlot();
        slot.setUser(user);
        apply(slot, request, linkedTask);
        return response(scheduleRepository.save(slot));
    }

    @Override @Transactional(readOnly = true)
    public SchedulePageResponse getAll(Long userId, int page, int size) {
        validatePage(page, size);
        Page<ScheduleSlot> result = scheduleRepository.findAllByUser_Id(userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "startDateTime")));
        return new SchedulePageResponse(result.getContent().stream().map(this::response).toList(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isLast());
    }

    @Override @Transactional(readOnly = true)
    public ScheduleSlotResponse getById(Long userId, Long id) { return response(findSlot(userId, id)); }

    @Override @Transactional
    public ScheduleSlotResponse update(Long userId, Long id, ScheduleSlotRequest request) {
        validateInterval(request.getStartDateTime(), request.getEndDateTime());
        ScheduleSlot slot = findSlot(userId, id);
        Task linkedTask = resolveTask(userId, request.getTaskId());
        ensureTaskNotAlreadyScheduled(userId, request.getTaskId(), id);
        ensureNoConflicts(userId, request.getStartDateTime(), request.getEndDateTime(), id, request.getTaskId());
        apply(slot, request, linkedTask);
        return response(scheduleRepository.save(slot));
    }

    @Override @Transactional
    public void delete(Long userId, Long id) { scheduleRepository.delete(findSlot(userId, id)); }

    @Override @Transactional(readOnly = true)
    public List<ScheduleSlotResponse> daily(Long userId, LocalDate date) {
        if (date == null) throw new BadRequestException("date is required");
        LocalDateTime start = date.atStartOfDay();
        return scheduleRepository.findOverlappingRange(userId, start, start.plusDays(1)).stream().map(this::response).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<ScheduleSlotResponse> weekly(Long userId, LocalDate startDate) {
        if (startDate == null) throw new BadRequestException("startDate is required");
        LocalDateTime start = startDate.atStartOfDay();
        return scheduleRepository.findOverlappingRange(userId, start, start.plusDays(7)).stream().map(this::response).toList();
    }

    @Override @Transactional(readOnly = true)
    public ScheduleConflictResponse detectConflicts(Long userId, ScheduleConflictRequest request) {
        validateInterval(request.getStartDateTime(), request.getEndDateTime());
        resolveTask(userId, request.getTaskId());
        List<ScheduleConflictItem> conflicts = findConflicts(userId, request.getStartDateTime(), request.getEndDateTime(), null, request.getTaskId());
        return new ScheduleConflictResponse(!conflicts.isEmpty(), conflicts);
    }

    @Override @Transactional
    public List<ScheduleSlotResponse> approveDailyPlan(Long userId, DailyScheduleApprovalRequest request) {
        if (request == null || request.getDate() == null) throw new BadRequestException("date is required");
        if (request.getDate().isBefore(LocalDate.now())) throw new BadRequestException("Plan date cannot be in the past");
        if (request.getItems() == null || request.getItems().isEmpty()) throw new BadRequestException("At least one schedule item is required");

        Set<Long> taskIds = new HashSet<>();
        for (DailyScheduleApprovalItem item : request.getItems()) {
            if (item == null || item.getTaskId() == null || item.getTaskId() <= 0
                    || item.getStartTime() == null || item.getEndTime() == null) {
                throw new BadRequestException("Each schedule item requires a valid task and start and end times");
            }
            if (!taskIds.add(item.getTaskId())) throw new BadRequestException("A task may appear only once in a daily plan");
            if (!item.getStartTime().isBefore(item.getEndTime())) {
                throw new BadRequestException("startTime must be before endTime");
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User account was not found"));
        List<ScheduleSlotResponse> approved = new ArrayList<>();
        for (DailyScheduleApprovalItem item : request.getItems()) {
            Task task = resolveTask(userId, item.getTaskId());
            if (task.getStatus() != com.lifesync.lifesync.task.entity.TaskStatus.TODO
                    && task.getStatus() != com.lifesync.lifesync.task.entity.TaskStatus.IN_PROGRESS) {
                throw new BadRequestException("Only pending tasks can be added to a daily plan");
            }

            LocalDateTime start = request.getDate().atTime(item.getStartTime());
            LocalDateTime end = request.getDate().atTime(item.getEndTime());
            List<ScheduleSlot> linkedSlots = scheduleRepository.findAllByUser_IdAndTask_IdOrderByStartDateTimeAsc(userId, task.getId());
            ScheduleSlot slot = linkedSlots.isEmpty() ? new ScheduleSlot() : linkedSlots.get(0);
            Long excludedSlotId = slot.getId();
            ensureNoConflicts(userId, start, end, excludedSlotId, task.getId());

            if (excludedSlotId == null) slot.setUser(user);
            slot.setTitle(task.getTitle());
            slot.setDescription("Approved from the daily plan.");
            slot.setStartDateTime(start);
            slot.setEndDateTime(end);
            slot.setTask(task);
            approved.add(response(scheduleRepository.save(slot)));
        }
        return approved;
    }

    private void ensureNoConflicts(Long userId, LocalDateTime start, LocalDateTime end, Long excludedSlotId, Long linkedTaskId) {
        List<ScheduleConflictItem> conflicts = findConflicts(userId, start, end, excludedSlotId, linkedTaskId);
        if (!conflicts.isEmpty()) throw new DuplicateResourceException("The proposed schedule slot overlaps an existing schedule slot or task interval");
    }

    private List<ScheduleConflictItem> findConflicts(Long userId, LocalDateTime start, LocalDateTime end, Long excludedSlotId, Long linkedTaskId) {
        List<ScheduleSlot> overlappingSlots = excludedSlotId == null
                ? scheduleRepository.findOverlappingRange(userId, start, end)
                : scheduleRepository.findOverlappingRangeExcludingId(userId, start, end, excludedSlotId);
        List<ScheduleConflictItem> conflicts = new ArrayList<>();
        for (ScheduleSlot slot : overlappingSlots) {
            conflicts.add(new ScheduleConflictItem("SCHEDULE_SLOT", slot.getId(), slot.getTitle(), slot.getStartDateTime(), slot.getEndDateTime()));
        }
        for (CalendarEventResponse event : calendarEventService.getEvents(userId, null, start, end)) {
            if (linkedTaskId != null && linkedTaskId.equals(event.getTaskId())) continue;
            conflicts.add(new ScheduleConflictItem("CALENDAR_EVENT", event.getId(), event.getTitle(),
                    event.getStartDateTime(), event.getEndDateTime()));
        }
        for (Task task : taskRepository.findAllByUser_IdAndStartDateLessThanAndDueDateGreaterThan(userId, end, start)) {
            if (linkedTaskId != null && linkedTaskId.equals(task.getId())) continue;
            conflicts.add(new ScheduleConflictItem("TASK", task.getId(), task.getTitle(), task.getStartDate(), task.getDueDate()));
        }
        return conflicts;
    }

    private Task resolveTask(Long userId, Long taskId) {
        if (taskId == null) return null;
        return taskRepository.findByIdAndUser_Id(taskId, userId).orElseThrow(() -> new ResourceNotFoundException("Task was not found"));
    }

    private void ensureTaskNotAlreadyScheduled(Long userId, Long taskId, Long currentSlotId) {
        if (taskId == null) return;
        boolean alreadyScheduled = scheduleRepository.findAllByUser_IdAndTask_IdOrderByStartDateTimeAsc(userId, taskId)
                .stream().anyMatch(slot -> !slot.getId().equals(currentSlotId));
        if (alreadyScheduled) throw new DuplicateResourceException("This task already has a schedule entry");
    }

    private ScheduleSlot findSlot(Long userId, Long id) {
        return scheduleRepository.findByIdAndUser_Id(id, userId).orElseThrow(() -> new ResourceNotFoundException("Schedule slot was not found"));
    }

    private void apply(ScheduleSlot slot, ScheduleSlotRequest request, Task task) {
        slot.setTitle(request.getTitle().trim());
        slot.setDescription(request.getDescription() == null || request.getDescription().isBlank() ? null : request.getDescription().trim());
        slot.setStartDateTime(request.getStartDateTime());
        slot.setEndDateTime(request.getEndDateTime());
        slot.setTask(task);
    }

    private void validateInterval(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) throw new BadRequestException("Start and end date-times are required");
        if (!start.isBefore(end)) throw new BadRequestException("startDateTime must be before endDateTime");
    }

    private void validatePage(int page, int size) {
        if (page < 0) throw new BadRequestException("Page must be zero or greater");
        if (size < 1 || size > ApiConstants.MAX_PAGE_SIZE) throw new BadRequestException("Page size must be between 1 and " + ApiConstants.MAX_PAGE_SIZE);
    }

    private ScheduleSlotResponse response(ScheduleSlot slot) {
        Task task = slot.getTask();
        return new ScheduleSlotResponse(slot.getId(), slot.getTitle(), slot.getDescription(), slot.getStartDateTime(), slot.getEndDateTime(),
                task == null ? null : task.getId(), task == null ? null : task.getTitle(), slot.getCreatedAt(), slot.getUpdatedAt());
    }
}

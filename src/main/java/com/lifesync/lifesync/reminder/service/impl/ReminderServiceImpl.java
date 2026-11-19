package com.lifesync.lifesync.reminder.service.impl;

import com.lifesync.lifesync.common.ApiConstants;
import com.lifesync.lifesync.exception.BadRequestException;
import com.lifesync.lifesync.exception.ResourceNotFoundException;
import com.lifesync.lifesync.notification.service.NotificationEventService;
import com.lifesync.lifesync.reminder.dto.*;
import com.lifesync.lifesync.reminder.entity.Reminder;
import com.lifesync.lifesync.reminder.entity.ReminderStatus;
import com.lifesync.lifesync.reminder.repository.ReminderRepository;
import com.lifesync.lifesync.reminder.service.ReminderService;
import com.lifesync.lifesync.task.entity.Task;
import com.lifesync.lifesync.task.entity.TaskStatus;
import com.lifesync.lifesync.task.repository.TaskRepository;
import com.lifesync.lifesync.user.entity.User;
import com.lifesync.lifesync.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReminderServiceImpl implements ReminderService {
    private final ReminderRepository reminders;
    private final NotificationEventService notificationEvents;
    private final UserRepository users;
    private final TaskRepository tasks;
    @Autowired
    public ReminderServiceImpl(ReminderRepository reminders,NotificationEventService notificationEvents,UserRepository users,TaskRepository tasks){this.reminders=reminders;this.notificationEvents=notificationEvents;this.users=users;this.tasks=tasks;}

    @Override @Transactional
    public ReminderResponse create(Long userId,ReminderRequest request){
        validate(request);User user=users.findById(userId).orElseThrow(()->new ResourceNotFoundException("User account was not found"));
        Reminder reminder=new Reminder();reminder.setUser(user);apply(reminder,request,userId);reminder.setStatus(ReminderStatus.PENDING);
        return response(reminders.save(reminder));
    }
    @Override @Transactional(readOnly=true)
    public ReminderPageResponse getAll(Long userId,int page,int size){
        validatePage(page,size);Page<Reminder> result=reminders.findAllByUser_Id(userId,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"reminderDateTime")));
        return new ReminderPageResponse(result.getContent().stream().map(this::response).toList(),result.getNumber(),result.getSize(),result.getTotalElements(),result.getTotalPages(),result.isLast());
    }
    @Override @Transactional(readOnly=true)
    public ReminderResponse getById(Long userId,Long id){return response(findReminder(userId,id));}
    @Override @Transactional
    public ReminderResponse update(Long userId,Long id,ReminderRequest request){
        validate(request);Reminder reminder=findReminder(userId,id);
        if(reminder.getStatus()!=ReminderStatus.PENDING)throw new BadRequestException("Only pending reminders can be updated");
        apply(reminder,request,userId);return response(reminders.save(reminder));
    }
    @Override @Transactional
    public void cancel(Long userId,Long id){
        Reminder reminder=findReminder(userId,id);
        if(reminder.getStatus()==ReminderStatus.PENDING){reminder.setStatus(ReminderStatus.CANCELLED);reminders.save(reminder);}
    }
    @Override @Transactional(readOnly=true)
    public ReminderPageResponse upcoming(Long userId,int page,int size){
        validatePage(page,size);
        Page<Reminder> result=reminders.findAllByUser_IdAndStatusAndReminderDateTimeGreaterThanEqual(userId,ReminderStatus.PENDING,LocalDateTime.now(),
                PageRequest.of(page,size,Sort.by(Sort.Direction.ASC,"reminderDateTime")));
        return new ReminderPageResponse(result.getContent().stream().map(this::response).toList(),result.getNumber(),result.getSize(),result.getTotalElements(),result.getTotalPages(),result.isLast());
    }
    @Override @Transactional
    public int processDueReminders(){
        List<Reminder> due=reminders.findTop100ByStatusAndReminderDateTimeLessThanEqualOrderByReminderDateTimeAsc(ReminderStatus.PENDING,LocalDateTime.now());
        int generated=0;
        for(Reminder reminder:due){
            if(reminder.getStatus()!=ReminderStatus.PENDING)continue;
            if(reminder.getTask()!=null && (reminder.getTask().getStatus()==TaskStatus.COMPLETED
                    || reminder.getTask().getStatus()==TaskStatus.CANCELLED
                    || reminder.getTask().getStatus()==TaskStatus.MISSED)){
                reminder.setStatus(ReminderStatus.CANCELLED);reminders.save(reminder);continue;
            }
            if(notificationEvents.reminderDue(reminder))generated++;
            reminder.setStatus(ReminderStatus.SENT);reminders.save(reminder);
        }
        return generated;
    }
    private Reminder findReminder(Long userId,Long id){return reminders.findByIdAndUser_Id(id,userId).orElseThrow(()->new ResourceNotFoundException("Reminder was not found"));}
    private void validate(ReminderRequest request){
        if(request.getTitle()==null||request.getTitle().isBlank())throw new BadRequestException("title is required");
        if(request.getMessage()==null||request.getMessage().isBlank())throw new BadRequestException("message is required");
        if(request.getReminderDateTime()==null)throw new BadRequestException("reminderDateTime is required");
        if(request.getReminderType()==null)throw new BadRequestException("reminderType is required");
        if(request.getDeadlineOffsetMinutes()!=null
                &&(request.getDeadlineOffsetMinutes()<0||request.getDeadlineOffsetMinutes()>525600))
            throw new BadRequestException("deadlineOffsetMinutes must be between 0 and 525600");
        if(request.getTaskId()==null&&request.getDeadlineOffsetMinutes()!=null)
            throw new BadRequestException("deadlineOffsetMinutes can only be used with a task-linked reminder");
    }
    private void validatePage(int page,int size){if(page<0)throw new BadRequestException("Page must be zero or greater");if(size<1||size>ApiConstants.MAX_PAGE_SIZE)throw new BadRequestException("Page size must be between 1 and " + ApiConstants.MAX_PAGE_SIZE);}
    private void apply(Reminder reminder,ReminderRequest request,Long userId){
        reminder.setTitle(request.getTitle().trim());reminder.setMessage(request.getMessage().trim());reminder.setReminderDateTime(request.getReminderDateTime());reminder.setReminderType(request.getReminderType());
        Task task=resolveTask(userId,request.getTaskId());
        reminder.setDeadlineOffsetMinutes(request.getDeadlineOffsetMinutes());
        if(task!=null&&request.getDeadlineOffsetMinutes()!=null)
            reminder.setReminderDateTime(task.getDueDate().minusMinutes(request.getDeadlineOffsetMinutes()));
        if(task!=null&&reminder.getReminderDateTime().isAfter(task.getDueDate()))
            throw new BadRequestException("Task-linked reminders must be scheduled no later than the task deadline");
        reminder.setTask(task);
    }
    private Task resolveTask(Long userId,Long taskId){
        if(taskId==null){return null;}
        if(tasks==null){throw new IllegalStateException("Task management is not configured");}
        Task task=tasks.findByIdAndUser_Id(taskId,userId).orElseThrow(()->new ResourceNotFoundException("Task was not found"));
        if(task.getDueDate()==null)throw new BadRequestException("Task-linked reminders require a task deadline");
        if(task.getStatus()==TaskStatus.COMPLETED||task.getStatus()==TaskStatus.CANCELLED||task.getStatus()==TaskStatus.MISSED)
            throw new BadRequestException("Cannot create reminders for a completed, missed, or cancelled task");
        return task;
    }
    private ReminderResponse response(Reminder r){
        Long taskId=r.getTask()==null?null:r.getTask().getId();
        String taskTitle=r.getTask()==null?null:r.getTask().getTitle();
        return new ReminderResponse(r.getId(),r.getTitle(),r.getMessage(),r.getReminderDateTime(),r.getStatus(),r.getReminderType(),taskId,taskTitle,r.getDeadlineOffsetMinutes(),r.getCreatedAt(),r.getUpdatedAt());
    }
}

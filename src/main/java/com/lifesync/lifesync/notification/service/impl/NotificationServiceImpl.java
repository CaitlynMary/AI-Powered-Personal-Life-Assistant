package com.lifesync.lifesync.notification.service.impl;

import com.lifesync.lifesync.common.ApiConstants;
import com.lifesync.lifesync.exception.BadRequestException;
import com.lifesync.lifesync.exception.ResourceNotFoundException;
import com.lifesync.lifesync.notification.dto.NotificationPageResponse;
import com.lifesync.lifesync.notification.dto.NotificationResponse;
import com.lifesync.lifesync.notification.entity.Notification;
import com.lifesync.lifesync.notification.repository.NotificationRepository;
import com.lifesync.lifesync.notification.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notifications;
    public NotificationServiceImpl(NotificationRepository notifications){this.notifications=notifications;}
    @Override @Transactional(readOnly=true)
    public NotificationPageResponse getAll(Long userId,int page,int size,boolean unreadOnly){
        validatePage(page,size);Page<Notification> result=unreadOnly
                ?notifications.findAllByUser_IdAndIsReadFalseOrderByCreatedAtDesc(userId,PageRequest.of(page,size))
                :notifications.findAllByUser_IdOrderByCreatedAtDesc(userId,PageRequest.of(page,size));
        return new NotificationPageResponse(result.getContent().stream().map(this::response).toList(),result.getNumber(),result.getSize(),result.getTotalElements(),result.getTotalPages(),result.isLast());
    }
    @Override @Transactional
    public void markRead(Long userId,Long id){Notification notification=notifications.findByIdAndUser_Id(id,userId).orElseThrow(()->new ResourceNotFoundException("Notification was not found"));notification.setIsRead(true);notifications.save(notification);}
    @Override @Transactional
    public int markAllRead(Long userId){return notifications.markAllRead(userId);}
    @Override @Transactional(readOnly=true)
    public long unreadCount(Long userId){return notifications.countByUser_IdAndIsReadFalse(userId);}
    private void validatePage(int page,int size){if(page<0)throw new BadRequestException("Page must be zero or greater");if(size<1||size>ApiConstants.MAX_PAGE_SIZE)throw new BadRequestException("Page size must be between 1 and " + ApiConstants.MAX_PAGE_SIZE);}
    private NotificationResponse response(Notification n){
        Long reminderId=n.getReminder()==null?null:n.getReminder().getId();
        String reminderTitle=n.getReminder()==null?null:n.getReminder().getTitle();
        Long taskId=n.getTask()==null?null:n.getTask().getId();
        String taskTitle=n.getTask()==null?null:n.getTask().getTitle();
        Long goalId=n.getGoal()==null?null:n.getGoal().getId();
        String goalTitle=n.getGoal()==null?null:n.getGoal().getTitle();
        Long milestoneId=n.getMilestone()==null?null:n.getMilestone().getId();
        String milestoneTitle=n.getMilestone()==null?null:n.getMilestone().getTitle();
        Long habitId=n.getHabit()==null?null:n.getHabit().getId();
        String habitName=n.getHabit()==null?null:n.getHabit().getName();
        return new NotificationResponse(n.getId(),n.getTitle(),n.getMessage(),n.getNotificationType(),n.getIsRead(),
                reminderId,reminderTitle,taskId,taskTitle,goalId,goalTitle,milestoneId,milestoneTitle,habitId,habitName,n.getCreatedAt());
    }
}

package com.lifesync.lifesync.milestone.service.impl;

import com.lifesync.lifesync.exception.BadRequestException;
import com.lifesync.lifesync.exception.ResourceNotFoundException;
import com.lifesync.lifesync.goal.entity.Goal;
import com.lifesync.lifesync.goal.entity.GoalStatus;
import com.lifesync.lifesync.goal.repository.GoalRepository;
import com.lifesync.lifesync.milestone.dto.MilestoneRequest;
import com.lifesync.lifesync.milestone.dto.MilestoneResponse;
import com.lifesync.lifesync.milestone.entity.GoalMilestone;
import com.lifesync.lifesync.milestone.entity.MilestoneLifecycleStatus;
import com.lifesync.lifesync.milestone.entity.MilestoneStatus;
import com.lifesync.lifesync.milestone.repository.GoalMilestoneRepository;
import com.lifesync.lifesync.milestone.service.MilestoneService;
import com.lifesync.lifesync.notification.service.NotificationEventService;
import com.lifesync.lifesync.task.repository.TaskRepository;
import com.lifesync.lifesync.task.entity.TaskStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MilestoneServiceImpl implements MilestoneService {
    private final GoalRepository goals;
    private final GoalMilestoneRepository milestones;
    private final TaskRepository tasks;
    private final NotificationEventService notificationEvents;
    public MilestoneServiceImpl(GoalRepository goals,GoalMilestoneRepository milestones,TaskRepository tasks){this(goals,milestones,tasks,null);}
    @org.springframework.beans.factory.annotation.Autowired
    public MilestoneServiceImpl(GoalRepository goals,GoalMilestoneRepository milestones,TaskRepository tasks,NotificationEventService notificationEvents){this.goals=goals;this.milestones=milestones;this.tasks=tasks;this.notificationEvents=notificationEvents;}
    @Override @Transactional
    public MilestoneResponse create(Long userId,Long goalId,MilestoneRequest request){
        Goal goal=findGoal(userId,goalId);validateDate(goal,request.getTargetDate());
        GoalMilestone milestone=new GoalMilestone();milestone.setGoal(goal);apply(milestone,request);milestone.setStatus(MilestoneStatus.NOT_STARTED);
        MilestoneResponse response=response(milestones.save(milestone));recalculateGoal(goal);return response;
    }
    @Override @Transactional(readOnly=true)
    public List<MilestoneResponse> getForGoal(Long userId,Long goalId){findGoal(userId,goalId);return milestones.findAllByGoal_IdOrderByTargetDateAsc(goalId).stream().map(this::response).toList();}
    @Override @Transactional(readOnly=true)
    public MilestoneResponse getById(Long userId,Long id){return response(findMilestone(userId,id));}
    @Override @Transactional
    public MilestoneResponse update(Long userId,Long id,MilestoneRequest request){
        GoalMilestone milestone=findMilestone(userId,id);validateDate(milestone.getGoal(),request.getTargetDate());apply(milestone,request);return response(milestones.save(milestone));
    }
    @Override @Transactional
    public MilestoneResponse updateStatus(Long userId,Long id,MilestoneStatus status){
        if(status==null)throw new BadRequestException("Milestone status is required");
        GoalMilestone milestone=findMilestone(userId,id);
        if(status==MilestoneStatus.COMPLETED)ensureLinkedTasksComplete(userId,milestone);
        if(milestone.getStatus()==status)return response(milestone);
        milestone.setStatus(status);
        milestone.setCompletedAt(status==MilestoneStatus.COMPLETED?LocalDateTime.now():null);
        GoalMilestone saved=milestones.save(milestone);
        if(status==MilestoneStatus.COMPLETED&&notificationEvents!=null)notificationEvents.milestoneCompleted(saved);
        MilestoneResponse response=response(saved);recalculateGoal(milestone.getGoal());return response;
    }
    @Override @Transactional
    public MilestoneResponse complete(Long userId,Long id){
        GoalMilestone milestone=findMilestone(userId,id);
        ensureLinkedTasksComplete(userId,milestone);
        boolean newlyCompleted=milestone.getStatus()!=MilestoneStatus.COMPLETED;
        if(newlyCompleted){milestone.setStatus(MilestoneStatus.COMPLETED);milestone.setCompletedAt(LocalDateTime.now());}
        GoalMilestone saved=milestones.save(milestone);
        if(newlyCompleted&&notificationEvents!=null)notificationEvents.milestoneCompleted(saved);
        MilestoneResponse response=response(saved);recalculateGoal(milestone.getGoal());return response;
    }
    @Override @Transactional
    public void synchronizeTaskProgress(Long userId,Long milestoneId){
        GoalMilestone milestone=findMilestone(userId,milestoneId);
        long totalTasks=tasks.countByMilestone_IdAndUser_Id(milestoneId,userId);
        if(totalTasks==0)return;
        long completedTasks=tasks.countByMilestone_IdAndUser_IdAndStatus(milestoneId,userId,TaskStatus.COMPLETED);
        MilestoneStatus nextStatus=completedTasks==totalTasks?MilestoneStatus.COMPLETED
                :completedTasks>0?MilestoneStatus.IN_PROGRESS:MilestoneStatus.NOT_STARTED;
        boolean newlyCompleted=milestone.getStatus()!=MilestoneStatus.COMPLETED
                &&nextStatus==MilestoneStatus.COMPLETED;
        if(milestone.getStatus()!=nextStatus){
            milestone.setStatus(nextStatus);
            milestone.setCompletedAt(nextStatus==MilestoneStatus.COMPLETED?LocalDateTime.now():null);
            milestone=milestones.save(milestone);
        }
        if(newlyCompleted&&notificationEvents!=null)notificationEvents.milestoneCompleted(milestone);
        recalculateGoal(milestone.getGoal());
    }
    @Override @Transactional
    public void delete(Long userId,Long id){
        GoalMilestone milestone=findMilestone(userId,id);Goal goal=milestone.getGoal();
        tasks.clearMilestone(userId,id);milestones.delete(milestone);recalculateGoal(goal);
    }
    private Goal findGoal(Long userId,Long id){return goals.findByIdAndUser_Id(id,userId).orElseThrow(()->new ResourceNotFoundException("Goal was not found"));}
    private GoalMilestone findMilestone(Long userId,Long id){return milestones.findByIdAndGoal_User_Id(id,userId).orElseThrow(()->new ResourceNotFoundException("Milestone was not found"));}
    private void validateDate(Goal goal,java.time.LocalDate date){if(date==null)throw new BadRequestException("Milestone targetDate is required");if(date.isBefore(goal.getStartDate())||date.isAfter(goal.getTargetDate()))throw new BadRequestException("Milestone targetDate must be within the goal date range");}
    private void apply(GoalMilestone milestone,MilestoneRequest request){milestone.setTitle(request.getTitle().trim());milestone.setDescription(request.getDescription()==null||request.getDescription().isBlank()?null:request.getDescription().trim());milestone.setTargetDate(request.getTargetDate());}
    private void recalculateGoal(Goal goal){
        long total=milestones.countByGoal_Id(goal.getId());long completed=milestones.countByGoal_IdAndStatus(goal.getId(),MilestoneStatus.COMPLETED);
        long inProgress=milestones.countByGoal_IdAndStatus(goal.getId(),MilestoneStatus.IN_PROGRESS);
        int progress=total==0?0:(int)Math.round(completed*100.0/total);goal.setProgressPercentage(Math.max(0,Math.min(100,progress)));
        if(total>0&&completed==total)goal.setStatus(GoalStatus.COMPLETED);
        else if(goal.getStatus()!=GoalStatus.CANCELLED)goal.setStatus(completed==0&&inProgress==0?GoalStatus.NOT_STARTED:GoalStatus.IN_PROGRESS);
        goals.save(goal);
    }
    private void ensureLinkedTasksComplete(Long userId,GoalMilestone milestone){
        long total=tasks.countByMilestone_IdAndUser_Id(milestone.getId(),userId);
        if(total>0&&tasks.countByMilestone_IdAndUser_IdAndStatus(milestone.getId(),userId,TaskStatus.COMPLETED)<total)
            throw new BadRequestException("Complete all linked tasks before completing this milestone");
    }
    private MilestoneResponse response(GoalMilestone m){return new MilestoneResponse(m.getId(),m.getGoal().getId(),m.getTitle(),m.getDescription(),m.getTargetDate(),MilestoneLifecycleStatus.of(m,LocalDate.now()),m.getCompletedAt(),m.getCreatedAt(),m.getUpdatedAt());}
}

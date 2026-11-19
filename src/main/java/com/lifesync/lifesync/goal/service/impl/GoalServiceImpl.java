package com.lifesync.lifesync.goal.service.impl;

import com.lifesync.lifesync.exception.BadRequestException;
import com.lifesync.lifesync.exception.ResourceNotFoundException;
import com.lifesync.lifesync.goal.dto.*;
import com.lifesync.lifesync.goal.entity.*;
import com.lifesync.lifesync.goal.repository.GoalRepository;
import com.lifesync.lifesync.goal.service.GoalService;
import com.lifesync.lifesync.milestone.entity.GoalMilestone;
import com.lifesync.lifesync.milestone.entity.MilestoneStatus;
import com.lifesync.lifesync.milestone.repository.GoalMilestoneRepository;
import com.lifesync.lifesync.task.repository.TaskRepository;
import com.lifesync.lifesync.user.entity.User;
import com.lifesync.lifesync.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
public class GoalServiceImpl implements GoalService {
    private final GoalRepository goals;
    private final GoalMilestoneRepository milestones;
    private final TaskRepository tasks;
    private final UserRepository users;
    public GoalServiceImpl(GoalRepository goals,GoalMilestoneRepository milestones,TaskRepository tasks,UserRepository users){this.goals=goals;this.milestones=milestones;this.tasks=tasks;this.users=users;}
    @Override @Transactional
    public GoalResponse create(Long userId,GoalRequest request){
        validateDates(request.getStartDate(),request.getTargetDate());
        User user=users.findById(userId).orElseThrow(()->new ResourceNotFoundException("User account was not found"));
        Goal goal=new Goal();goal.setUser(user);apply(goal,request);goal.setStatus(GoalStatus.NOT_STARTED);goal.setProgressPercentage(0);
        return response(goals.save(goal));
    }
    @Override @Transactional(readOnly=true)
    public List<GoalResponse> getAll(Long userId){return goals.findAllByUser_IdOrderByTargetDateAsc(userId).stream().map(this::response).toList();}
    @Override @Transactional(readOnly=true)
    public GoalResponse getById(Long userId,Long id){return response(findGoal(userId,id));}
    @Override @Transactional
    public GoalResponse update(Long userId,Long id,GoalRequest request){
        validateDates(request.getStartDate(),request.getTargetDate());Goal goal=findGoal(userId,id);
        boolean milestoneOutOfRange=milestones.findAllByGoal_Id(id).stream().anyMatch(m->m.getTargetDate().isBefore(request.getStartDate())||m.getTargetDate().isAfter(request.getTargetDate()));
        if(milestoneOutOfRange)throw new BadRequestException("Goal dates must contain all existing milestone target dates");
        apply(goal,request);return response(goals.save(goal));
    }
    @Override @Transactional
    public GoalResponse updateStatus(Long userId,Long id,GoalStatus status){
        if(status==null)throw new BadRequestException("Goal status is required");
        Goal goal=findGoal(userId,id);
        boolean reversingCompletion=goal.getStatus()==GoalStatus.COMPLETED
                &&(status==GoalStatus.NOT_STARTED||status==GoalStatus.IN_PROGRESS);
        if(status==GoalStatus.NOT_STARTED&&goal.getProgressPercentage()>0&&!reversingCompletion)throw new BadRequestException("A goal with milestone progress cannot be reset to NOT_STARTED");
        if(status==GoalStatus.COMPLETED&&milestones.countByGoal_IdAndStatus(id,com.lifesync.lifesync.milestone.entity.MilestoneStatus.COMPLETED)<milestones.countByGoal_Id(id))
            throw new BadRequestException("Complete all goal milestones before marking the goal completed");
        if(reversingCompletion)resetCompletedMilestones(id);
        goal.setStatus(status);
        if(status==GoalStatus.COMPLETED)goal.setProgressPercentage(100);
        else if(reversingCompletion)goal.setProgressPercentage(0);
        return response(goals.save(goal));
    }
    @Override @Transactional
    public void delete(Long userId,Long id){
        Goal goal=findGoal(userId,id);
        tasks.clearGoalReferences(userId,id);
        milestones.deleteAllForGoal(id);
        goals.delete(goal);
    }
    @Override @Transactional(readOnly=true)
    public GoalStatisticsResponse statistics(Long userId){
        List<Goal> ownGoals=goals.findAllByUser_IdOrderByTargetDateAsc(userId);
        long total=ownGoals.size();
        double average=total==0?0.0:ownGoals.stream().mapToInt(g->g.getProgressPercentage()==null?0:g.getProgressPercentage()).average().orElse(0.0);
        return new GoalStatisticsResponse(total,
                goals.countByUser_IdAndStatus(userId,GoalStatus.NOT_STARTED),
                goals.countByUser_IdAndStatus(userId,GoalStatus.IN_PROGRESS),
                goals.countByUser_IdAndStatus(userId,GoalStatus.COMPLETED),
                goals.countByUser_IdAndStatus(userId,GoalStatus.CANCELLED),
                goals.countByUser_IdAndTargetDateBeforeAndStatusNotIn(userId,LocalDate.now(),List.of(GoalStatus.COMPLETED,GoalStatus.CANCELLED)),average);
    }
    private Goal findGoal(Long userId,Long id){return goals.findByIdAndUser_Id(id,userId).orElseThrow(()->new ResourceNotFoundException("Goal was not found"));}
    private void validateDates(LocalDate start,LocalDate target){if(start==null||target==null)throw new BadRequestException("Goal startDate and targetDate are required");if(target.isBefore(start))throw new BadRequestException("targetDate cannot be before startDate");}
    private void resetCompletedMilestones(Long goalId){
        List<GoalMilestone> goalMilestones=milestones.findAllByGoal_Id(goalId);
        for(GoalMilestone milestone:goalMilestones){
            if(milestone.getStatus()==MilestoneStatus.COMPLETED){
                milestone.setStatus(MilestoneStatus.NOT_STARTED);
                milestone.setCompletedAt(null);
            }
        }
        milestones.saveAll(goalMilestones);
    }
    private void apply(Goal goal,GoalRequest request){goal.setTitle(request.getTitle().trim());goal.setDescription(request.getDescription()==null||request.getDescription().isBlank()?null:request.getDescription().trim());goal.setGoalType(request.getGoalType());goal.setStartDate(request.getStartDate());goal.setTargetDate(request.getTargetDate());}
    private GoalResponse response(Goal g){return new GoalResponse(g.getId(),g.getTitle(),g.getDescription(),g.getGoalType(),g.getStartDate(),g.getTargetDate(),GoalLifecycleStatus.of(g,LocalDate.now()),g.getProgressPercentage(),g.getCreatedAt(),g.getUpdatedAt());}
}

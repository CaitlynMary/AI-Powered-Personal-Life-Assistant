package com.lifesync.lifesync.milestone.service;

import com.lifesync.lifesync.exception.BadRequestException;
import com.lifesync.lifesync.exception.ResourceNotFoundException;
import com.lifesync.lifesync.goal.entity.*;
import com.lifesync.lifesync.goal.repository.GoalRepository;
import com.lifesync.lifesync.milestone.dto.MilestoneRequest;
import com.lifesync.lifesync.milestone.entity.*;
import com.lifesync.lifesync.milestone.repository.GoalMilestoneRepository;
import com.lifesync.lifesync.milestone.service.impl.MilestoneServiceImpl;
import com.lifesync.lifesync.notification.service.NotificationEventService;
import com.lifesync.lifesync.task.repository.TaskRepository;
import com.lifesync.lifesync.task.entity.TaskStatus;
import com.lifesync.lifesync.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MilestoneServiceImplTest {
    @Mock GoalRepository goals; @Mock GoalMilestoneRepository milestones; @Mock TaskRepository tasks;
    @Mock NotificationEventService notificationEvents;
    private MilestoneServiceImpl service; private Goal goal;
    @BeforeEach void setUp(){service=new MilestoneServiceImpl(goals,milestones,tasks,notificationEvents);User user=new User();user.setId(2L);goal=new Goal();goal.setId(4L);goal.setUser(user);goal.setStartDate(LocalDate.of(2026,1,1));goal.setTargetDate(LocalDate.of(2026,12,31));goal.setProgressPercentage(0);goal.setStatus(GoalStatus.NOT_STARTED);}

    @Test void rejectsMissingOrOtherUsersGoal() {
        when(goals.findByIdAndUser_Id(4L,2L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->service.create(2L,4L,request(LocalDate.of(2026,4,1))));
    }

    @Test void rejectsMilestoneDateOutsideGoalDates() {
        when(goals.findByIdAndUser_Id(4L,2L)).thenReturn(Optional.of(goal));
        assertThrows(BadRequestException.class,()->service.create(2L,4L,request(LocalDate.of(2027,1,1))));
    }

    @Test void calculatesOverdueForUnfinishedMilestoneButNotCompletedMilestone() {
        GoalMilestone overdue=milestone(1L,MilestoneStatus.IN_PROGRESS);
        overdue.setTargetDate(LocalDate.now().minusDays(1));
        when(milestones.findByIdAndGoal_User_Id(1L,2L)).thenReturn(Optional.of(overdue));

        var response=service.getById(2L,1L);

        assertEquals(MilestoneLifecycleStatus.OVERDUE,response.getStatus());
        assertEquals(MilestoneStatus.IN_PROGRESS,overdue.getStatus());
        overdue.setStatus(MilestoneStatus.COMPLETED);
        assertEquals(MilestoneLifecycleStatus.COMPLETED,service.getById(2L,1L).getStatus());
    }

    @Test void completingMilestonesCalculatesProgressAndGoalStatus() {
        GoalMilestone first=milestone(1L,MilestoneStatus.NOT_STARTED);GoalMilestone second=milestone(2L,MilestoneStatus.NOT_STARTED);
        when(milestones.findByIdAndGoal_User_Id(1L,2L)).thenReturn(Optional.of(first));
        when(milestones.findByIdAndGoal_User_Id(2L,2L)).thenReturn(Optional.of(second));
        when(milestones.save(any())).thenAnswer(i->i.getArgument(0));when(goals.save(any())).thenAnswer(i->i.getArgument(0));
        when(milestones.countByGoal_Id(4L)).thenReturn(2L);when(milestones.countByGoal_IdAndStatus(4L,MilestoneStatus.COMPLETED)).thenReturn(1L,2L);
        assertEquals(50,service.complete(2L,1L).getGoalId()==4L?goal.getProgressPercentage():-1);
        assertEquals(GoalStatus.IN_PROGRESS,goal.getStatus());
        var secondResponse=service.complete(2L,2L);
        assertEquals(MilestoneLifecycleStatus.COMPLETED,secondResponse.getStatus());assertEquals(100,goal.getProgressPercentage());assertEquals(GoalStatus.COMPLETED,goal.getStatus());
        verify(notificationEvents,times(2)).milestoneCompleted(any(GoalMilestone.class));
    }

    @Test void reversingAndRepeatingMilestoneCompletionRecalculatesOnlyThatMilestone() {
        GoalMilestone first=milestone(1L,MilestoneStatus.COMPLETED);
        first.setCompletedAt(java.time.LocalDateTime.of(2026,3,1,10,0));
        GoalMilestone second=milestone(2L,MilestoneStatus.COMPLETED);
        goal.setStatus(GoalStatus.COMPLETED);goal.setProgressPercentage(100);
        when(milestones.findByIdAndGoal_User_Id(1L,2L)).thenReturn(Optional.of(first));
        when(milestones.save(any())).thenAnswer(i->i.getArgument(0));
        when(goals.save(any())).thenAnswer(i->i.getArgument(0));
        when(milestones.countByGoal_Id(4L)).thenReturn(2L);
        when(milestones.countByGoal_IdAndStatus(4L,MilestoneStatus.COMPLETED)).thenReturn(1L,2L);

        var reversed=service.updateStatus(2L,1L,MilestoneStatus.NOT_STARTED);

        assertEquals(MilestoneLifecycleStatus.OVERDUE,reversed.getStatus());
        assertNull(reversed.getCompletedAt());
        assertEquals(MilestoneStatus.COMPLETED,second.getStatus());
        assertEquals(50,goal.getProgressPercentage());
        assertEquals(GoalStatus.IN_PROGRESS,goal.getStatus());

        var completedAgain=service.updateStatus(2L,1L,MilestoneStatus.COMPLETED);
        assertEquals(MilestoneLifecycleStatus.COMPLETED,completedAgain.getStatus());
        assertEquals(100,goal.getProgressPercentage());
        assertEquals(GoalStatus.COMPLETED,goal.getStatus());
    }

    @Test void repeatedCompletionIsIdempotentAndCrossUserMilestoneIsHidden() {
        GoalMilestone completed=milestone(1L,MilestoneStatus.COMPLETED);completed.setCompletedAt(java.time.LocalDateTime.of(2026,3,1,10,0));
        when(milestones.findByIdAndGoal_User_Id(1L,2L)).thenReturn(Optional.of(completed));
        when(milestones.save(completed)).thenReturn(completed);when(goals.save(goal)).thenReturn(goal);
        when(milestones.countByGoal_Id(4L)).thenReturn(1L);when(milestones.countByGoal_IdAndStatus(4L,MilestoneStatus.COMPLETED)).thenReturn(1L);
        var response=service.complete(2L,1L);assertEquals(completed.getCompletedAt(),response.getCompletedAt());
        verify(notificationEvents,never()).milestoneCompleted(any(GoalMilestone.class));
        when(milestones.findByIdAndGoal_User_Id(8L,2L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->service.getById(2L,8L));
    }

    @Test void linkedTaskProgressCompletesMilestoneOnlyWhenAllTasksCompleteAndReversesOnReopen() {
        GoalMilestone milestone=milestone(1L,MilestoneStatus.NOT_STARTED);
        when(milestones.findByIdAndGoal_User_Id(1L,2L)).thenReturn(Optional.of(milestone));
        when(tasks.countByMilestone_IdAndUser_Id(1L,2L)).thenReturn(2L,2L,2L);
        when(tasks.countByMilestone_IdAndUser_IdAndStatus(1L,2L,TaskStatus.COMPLETED))
                .thenReturn(1L,2L,1L);
        when(milestones.save(any())).thenAnswer(i->i.getArgument(0));
        when(milestones.countByGoal_Id(4L)).thenReturn(1L);
        when(milestones.countByGoal_IdAndStatus(4L,MilestoneStatus.COMPLETED)).thenReturn(0L,1L,0L);
        when(milestones.countByGoal_IdAndStatus(4L,MilestoneStatus.IN_PROGRESS)).thenReturn(1L,0L,1L);
        when(goals.save(any())).thenAnswer(i->i.getArgument(0));

        service.synchronizeTaskProgress(2L,1L);
        assertEquals(MilestoneStatus.IN_PROGRESS,milestone.getStatus());
        assertEquals(GoalStatus.IN_PROGRESS,goal.getStatus());
        assertNull(milestone.getCompletedAt());

        service.synchronizeTaskProgress(2L,1L);
        assertEquals(MilestoneStatus.COMPLETED,milestone.getStatus());
        assertNotNull(milestone.getCompletedAt());
        assertEquals(100,goal.getProgressPercentage());
        assertEquals(GoalStatus.COMPLETED,goal.getStatus());

        service.synchronizeTaskProgress(2L,1L);
        assertEquals(MilestoneStatus.IN_PROGRESS,milestone.getStatus());
        assertNull(milestone.getCompletedAt());
        assertEquals(0,goal.getProgressPercentage());
        assertEquals(GoalStatus.IN_PROGRESS,goal.getStatus());
        verify(notificationEvents,times(1)).milestoneCompleted(milestone);
    }

    @Test void doesNotAutoCompleteMilestoneWithoutLinkedTasks() {
        GoalMilestone milestone=milestone(1L,MilestoneStatus.NOT_STARTED);
        when(milestones.findByIdAndGoal_User_Id(1L,2L)).thenReturn(Optional.of(milestone));
        when(tasks.countByMilestone_IdAndUser_Id(1L,2L)).thenReturn(0L);

        service.synchronizeTaskProgress(2L,1L);

        assertEquals(MilestoneStatus.NOT_STARTED,milestone.getStatus());
        verify(milestones,never()).save(any());
        verify(goals,never()).save(any());
    }

    @Test void rejectsManualMilestoneCompletionWhileLinkedTasksRemainIncomplete() {
        GoalMilestone milestone=milestone(1L,MilestoneStatus.NOT_STARTED);
        when(milestones.findByIdAndGoal_User_Id(1L,2L)).thenReturn(Optional.of(milestone));
        when(tasks.countByMilestone_IdAndUser_Id(1L,2L)).thenReturn(2L);
        when(tasks.countByMilestone_IdAndUser_IdAndStatus(1L,2L,TaskStatus.COMPLETED)).thenReturn(1L);

        assertThrows(BadRequestException.class,()->service.complete(2L,1L));
        assertThrows(BadRequestException.class,()->service.updateStatus(2L,1L,MilestoneStatus.COMPLETED));
        assertEquals(MilestoneStatus.NOT_STARTED,milestone.getStatus());
        verify(milestones,never()).save(any());
    }

    @Test void deletingMilestoneUnlinksTasksAndRecalculatesProgress() {
        GoalMilestone milestone=milestone(1L,MilestoneStatus.NOT_STARTED);
        when(milestones.findByIdAndGoal_User_Id(1L,2L)).thenReturn(Optional.of(milestone));
        when(milestones.countByGoal_Id(4L)).thenReturn(0L);when(milestones.countByGoal_IdAndStatus(4L,MilestoneStatus.COMPLETED)).thenReturn(0L);
        when(goals.save(goal)).thenReturn(goal);
        service.delete(2L,1L);
        var order=inOrder(tasks,milestones,goals);order.verify(tasks).clearMilestone(2L,1L);order.verify(milestones).delete(milestone);order.verify(goals).save(goal);
    }

    private MilestoneRequest request(LocalDate date){return new MilestoneRequest("Milestone",null,date);}
    private GoalMilestone milestone(Long id,MilestoneStatus status){GoalMilestone m=new GoalMilestone();m.setId(id);m.setGoal(goal);m.setTitle("M"+id);m.setTargetDate(LocalDate.of(2026,5,1));m.setStatus(status);return m;}
}

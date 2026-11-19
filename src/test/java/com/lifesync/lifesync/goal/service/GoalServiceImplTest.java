package com.lifesync.lifesync.goal.service;

import com.lifesync.lifesync.exception.BadRequestException;
import com.lifesync.lifesync.exception.ResourceNotFoundException;
import com.lifesync.lifesync.goal.dto.GoalRequest;
import com.lifesync.lifesync.goal.entity.*;
import com.lifesync.lifesync.goal.repository.GoalRepository;
import com.lifesync.lifesync.goal.service.impl.GoalServiceImpl;
import com.lifesync.lifesync.milestone.entity.GoalMilestone;
import com.lifesync.lifesync.milestone.entity.MilestoneStatus;
import com.lifesync.lifesync.milestone.repository.GoalMilestoneRepository;
import com.lifesync.lifesync.task.repository.TaskRepository;
import com.lifesync.lifesync.user.entity.User;
import com.lifesync.lifesync.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalServiceImplTest {
    @Mock GoalRepository goals;
    @Mock GoalMilestoneRepository milestones;
    @Mock TaskRepository tasks;
    @Mock UserRepository users;
    private GoalServiceImpl service;
    private User user;
    @BeforeEach void setUp(){service=new GoalServiceImpl(goals,milestones,tasks,users);user=new User();user.setId(5L);}

    @Test void createsGoalForPrincipalAndStartsAtZero() {
        when(users.findById(5L)).thenReturn(Optional.of(user));
        when(goals.save(any(Goal.class))).thenAnswer(i->{Goal g=i.getArgument(0);g.setId(9L);return g;});
        var response=service.create(5L,request());
        assertEquals(9L,response.getId());assertEquals(GoalLifecycleStatus.NOT_STARTED,response.getStatus());assertEquals(0,response.getProgressPercentage());
        verify(goals).save(argThat(g->g.getUser().getId().equals(5L)));
    }

    @Test void hidesGoalOwnedByAnotherUser() {
        when(goals.findByIdAndUser_Id(9L,5L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->service.getById(5L,9L));
        assertThrows(ResourceNotFoundException.class,()->service.delete(5L,9L));
    }

    @Test void rejectsTargetBeforeStartAndMilestoneRangeExclusion() {
        GoalRequest invalid=new GoalRequest("Goal",null,GoalType.MONTHLY,LocalDate.of(2026,5,10),LocalDate.of(2026,5,1));
        assertThrows(BadRequestException.class,()->service.create(5L,invalid));
        Goal goal=goal();when(goals.findByIdAndUser_Id(9L,5L)).thenReturn(Optional.of(goal));
        var milestone=new com.lifesync.lifesync.milestone.entity.GoalMilestone();milestone.setTargetDate(LocalDate.of(2026,6,2));
        when(milestones.findAllByGoal_Id(9L)).thenReturn(List.of(milestone));
        GoalRequest narrowed=new GoalRequest("Goal",null,GoalType.MONTHLY,LocalDate.of(2026,6,1),LocalDate.of(2026,6,1));
        assertThrows(BadRequestException.class,()->service.update(5L,9L,narrowed));
    }

    @Test void statusCompletionSetsFullProgressAndRejectsResetWithProgress() {
        Goal goal=goal();goal.setProgressPercentage(25);
        when(goals.findByIdAndUser_Id(9L,5L)).thenReturn(Optional.of(goal));when(goals.save(goal)).thenReturn(goal);
        assertThrows(BadRequestException.class,()->service.updateStatus(5L,9L,GoalStatus.NOT_STARTED));
        when(milestones.countByGoal_Id(9L)).thenReturn(0L);
        when(milestones.countByGoal_IdAndStatus(9L,com.lifesync.lifesync.milestone.entity.MilestoneStatus.COMPLETED)).thenReturn(0L);
        assertEquals(100,service.updateStatus(5L,9L,GoalStatus.COMPLETED).getProgressPercentage());
    }

    @Test void reversingCompletionResetsMilestonesAndProgress() {
        Goal goal=goal();goal.setStatus(GoalStatus.COMPLETED);goal.setProgressPercentage(100);
        GoalMilestone milestone=new GoalMilestone();milestone.setStatus(MilestoneStatus.COMPLETED);
        milestone.setCompletedAt(LocalDateTime.now());
        when(goals.findByIdAndUser_Id(9L,5L)).thenReturn(Optional.of(goal));
        when(milestones.findAllByGoal_Id(9L)).thenReturn(List.of(milestone));
        when(goals.save(goal)).thenReturn(goal);

        var response=service.updateStatus(5L,9L,GoalStatus.NOT_STARTED);

        assertEquals(GoalLifecycleStatus.NOT_STARTED,response.getStatus());
        assertEquals(0,response.getProgressPercentage());
        assertEquals(MilestoneStatus.NOT_STARTED,milestone.getStatus());
        assertNull(milestone.getCompletedAt());
        verify(milestones).saveAll(List.of(milestone));
    }

    @Test void updatesOwnedGoal() {
        Goal goal=goal();when(goals.findByIdAndUser_Id(9L,5L)).thenReturn(Optional.of(goal));
        when(milestones.findAllByGoal_Id(9L)).thenReturn(List.of());when(goals.save(goal)).thenReturn(goal);
        GoalRequest changed=new GoalRequest("Updated",null,GoalType.MONTHLY,LocalDate.of(2026,2,1),LocalDate.of(2026,10,1));
        assertEquals("Updated",service.update(5L,9L,changed).getTitle());
    }

    @Test void calculatesOverdueWithoutChangingGoalStateOrProgressAndClearsItAfterExtension() {
        Goal goal=goal();
        goal.setStatus(GoalStatus.IN_PROGRESS);
        goal.setProgressPercentage(45);
        goal.setTargetDate(LocalDate.now().minusDays(1));
        when(goals.findByIdAndUser_Id(9L,5L)).thenReturn(Optional.of(goal),Optional.of(goal));
        when(milestones.findAllByGoal_Id(9L)).thenReturn(List.of());
        when(goals.save(goal)).thenReturn(goal);

        var overdue=service.getById(5L,9L);
        assertEquals(GoalLifecycleStatus.OVERDUE,overdue.getStatus());
        assertEquals(GoalStatus.IN_PROGRESS,goal.getStatus());
        assertEquals(45,overdue.getProgressPercentage());

        GoalRequest extension=new GoalRequest("Goal",null,GoalType.YEARLY,
                goal.getStartDate(),LocalDate.now().plusDays(30));
        var extended=service.update(5L,9L,extension);

        assertEquals(GoalLifecycleStatus.IN_PROGRESS,extended.getStatus());
        assertEquals(45,extended.getProgressPercentage());
    }

    @Test void computesStatisticsForOnlyTheRequestedOwner() {
        when(goals.findAllByUser_IdOrderByTargetDateAsc(5L)).thenReturn(List.of(goal()));
        when(goals.countByUser_IdAndStatus(5L,GoalStatus.NOT_STARTED)).thenReturn(1L);
        when(goals.countByUser_IdAndTargetDateBeforeAndStatusNotIn(eq(5L),any(),anyCollection())).thenReturn(1L);
        var statistics=service.statistics(5L);
        assertEquals(1L,statistics.getTotalGoals());assertEquals(1L,statistics.getNotStarted());assertEquals(1L,statistics.getOverdue());
    }

    @Test void deletionUnlinksTasksAndRemovesMilestonesWithoutDeletingTasks() {
        when(goals.findByIdAndUser_Id(9L,5L)).thenReturn(Optional.of(goal()));
        service.delete(5L,9L);
        var order=inOrder(tasks,milestones,goals);
        order.verify(tasks).clearGoalReferences(5L,9L);order.verify(milestones).deleteAllForGoal(9L);order.verify(goals).delete(any(Goal.class));
    }

    private GoalRequest request(){return new GoalRequest("Learn Java","Complete course",GoalType.YEARLY,LocalDate.now().minusMonths(1),LocalDate.now().plusMonths(1));}
    private Goal goal(){Goal g=new Goal();g.setId(9L);g.setUser(user);g.setTitle("Goal");g.setGoalType(GoalType.YEARLY);g.setStartDate(LocalDate.now().minusMonths(1));g.setTargetDate(LocalDate.now().plusMonths(1));g.setStatus(GoalStatus.NOT_STARTED);g.setProgressPercentage(0);return g;}
}

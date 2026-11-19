import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../core/api.config';
import { ConfirmationDialogService } from '../../shared/confirmation-dialog/confirmation-dialog.service';
import { GoalResponse, MilestoneResponse } from './data/goal.models';
import { GoalsPage } from './goals.page';

const goal: GoalResponse = {
  id: 12,
  title: 'Learn Angular',
  description: 'Build a working app',
  goalType: 'MONTHLY',
  startDate: '2026-06-01',
  targetDate: '2026-06-30',
  status: 'IN_PROGRESS',
  progressPercentage: 50,
  createdAt: '2026-06-01T10:00:00',
  updatedAt: '2026-06-02T10:00:00',
};

const milestone: MilestoneResponse = {
  id: 21,
  goalId: goal.id,
  title: 'Finish first lesson',
  description: 'Study the fundamentals',
  targetDate: '2026-06-15',
  status: 'NOT_STARTED',
  completedAt: null,
  createdAt: '2026-06-01T10:00:00',
  updatedAt: '2026-06-01T10:00:00',
};

describe('GoalsPage', () => {
  let fixture: ComponentFixture<GoalsPage>;
  let component: GoalsPage;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    fixture = TestBed.createComponent(GoalsPage);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function flushGoals(response: GoalResponse[], milestones: MilestoneResponse[][] = []): void {
    fixture.detectChanges();
    http.expectOne(`${API_BASE_URL}/api/goals`).flush(response);
    response.forEach((item, index) => {
      http
        .expectOne(`${API_BASE_URL}/api/goals/${item.id}/milestones`)
        .flush(milestones[index] ?? []);
    });
    fixture.detectChanges();
  }

  it('shows loading and a friendly empty state', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Loading your goals');
    http.expectOne(`${API_BASE_URL}/api/goals`).flush([]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No goals yet');
  });

  it('loads real goal and milestone details and filters supported status and type', () => {
    flushGoals([goal], [[milestone]]);

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Learn Angular');
    expect(text).toContain('Build a working app');
    expect(text).toContain('Monthly');
    expect(text).toContain('50%');
    expect(text).toContain('1 milestone');
    expect(text).toContain('Finish first lesson');
    expect(text).toContain('Study the fundamentals');

    component.updateStatusFilter('COMPLETED');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No goals match these filters');
  });

  it('displays overdue lifecycle state for goals and milestones without hiding progress', () => {
    const overdueGoal = { ...goal, status: 'OVERDUE' as const, progressPercentage: 50 };
    const overdueMilestone = { ...milestone, status: 'OVERDUE' as const };
    flushGoals([overdueGoal], [[overdueMilestone]]);

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Overdue');
    expect(text).toContain('This deadline has passed.');
    expect(text).toContain('50%');
    expect(text).toContain('Finish first lesson');
    expect(text).toContain('Extend deadline');
    expect(text).toContain('Continue working');
    expect(text).toContain('Cancel goal');
  });

  it('searches goal titles and descriptions case-insensitively with partial matches', () => {
    flushGoals([goal], [[milestone]]);

    component.searchText.set('  LEARN ANG  ');
    expect(component.visibleGoals().map((item) => item.id)).toEqual([goal.id]);
    component.searchText.set('ILD A WORK');
    expect(component.visibleGoals().map((item) => item.id)).toEqual([goal.id]);
    component.searchText.set('no-such-goal');
    expect(component.visibleGoals()).toEqual([]);
    component.searchText.set('');
    expect(component.visibleGoals().map((item) => item.id)).toEqual([goal.id]);
  });

  it('shows an error and retry when goal loading fails', () => {
    fixture.detectChanges();
    http
      .expectOne(`${API_BASE_URL}/api/goals`)
      .flush({ message: 'Goal service unavailable' }, { status: 503, statusText: 'Unavailable' });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Goal service unavailable');
    expect(fixture.nativeElement.textContent).toContain('Retry');
  });

  it('refreshes backend goal progress after checking a milestone', () => {
    const initialMilestone = { ...milestone, status: 'NOT_STARTED' as const };
    flushGoals([{ ...goal, status: 'NOT_STARTED', progressPercentage: 0 }], [[initialMilestone]]);

    component.toggleMilestoneCompletion(initialMilestone);
    const completeRequest = http.expectOne(`${API_BASE_URL}/api/milestones/21/status`);
    expect(completeRequest.request.method).toBe('PATCH');
    expect(completeRequest.request.body).toEqual({ status: 'COMPLETED' });
    completeRequest.flush({ ...initialMilestone, status: 'COMPLETED' });

    http
      .expectOne(`${API_BASE_URL}/api/goals`)
      .flush([{ ...goal, status: 'COMPLETED', progressPercentage: 100 }]);
    http
      .expectOne(`${API_BASE_URL}/api/goals/12/milestones`)
      .flush([{ ...initialMilestone, status: 'COMPLETED' }]);
    fixture.detectChanges();

    expect(component.goals()[0].progressPercentage).toBe(100);
    expect(component.goals()[0].status).toBe('COMPLETED');
    expect(fixture.nativeElement.textContent).toContain('Milestone completed.');
    expect(fixture.nativeElement.textContent).toContain('100%');
  });

  it('unchecks a completed milestone, refreshes progress, and allows it to be checked again', () => {
    const firstCompleted = {
      ...milestone,
      status: 'COMPLETED' as const,
      completedAt: '2026-06-10T10:00:00',
    };
    const secondCompleted = {
      ...milestone,
      id: 22,
      title: 'Finish second lesson',
      status: 'COMPLETED' as const,
      completedAt: '2026-06-11T10:00:00',
    };
    const completedGoal = { ...goal, status: 'COMPLETED' as const, progressPercentage: 100 };
    flushGoals([completedGoal], [[firstCompleted, secondCompleted]]);

    component.toggleMilestoneCompletion(firstCompleted);
    const uncheck = http.expectOne(`${API_BASE_URL}/api/milestones/21/status`);
    expect(uncheck.request.body).toEqual({ status: 'NOT_STARTED' });
    uncheck.flush({ ...firstCompleted, status: 'NOT_STARTED', completedAt: null });
    http
      .expectOne(`${API_BASE_URL}/api/goals`)
      .flush([{ ...goal, status: 'IN_PROGRESS', progressPercentage: 50 }]);
    http
      .expectOne(`${API_BASE_URL}/api/goals/12/milestones`)
      .flush([{ ...firstCompleted, status: 'NOT_STARTED', completedAt: null }, secondCompleted]);

    expect(component.goals()[0].status).toBe('IN_PROGRESS');
    expect(component.goals()[0].progressPercentage).toBe(50);
    expect(component.milestonesFor(goal.id).map((item) => item.status)).toEqual([
      'NOT_STARTED',
      'COMPLETED',
    ]);

    component.toggleMilestoneCompletion(component.milestonesFor(goal.id)[0]);
    const recheck = http.expectOne(`${API_BASE_URL}/api/milestones/21/status`);
    expect(recheck.request.body).toEqual({ status: 'COMPLETED' });
    recheck.flush(firstCompleted);
    http.expectOne(`${API_BASE_URL}/api/goals`).flush([completedGoal]);
    http
      .expectOne(`${API_BASE_URL}/api/goals/12/milestones`)
      .flush([firstCompleted, secondCompleted]);

    expect(component.goals()[0].status).toBe('COMPLETED');
    expect(component.goals()[0].progressPercentage).toBe(100);
  });

  it('keeps the persisted milestone state and reports an error when toggling fails', () => {
    const completedMilestone = { ...milestone, status: 'COMPLETED' as const };
    const completedGoal = { ...goal, status: 'COMPLETED' as const, progressPercentage: 100 };
    flushGoals([completedGoal], [[completedMilestone]]);

    component.toggleMilestoneCompletion(completedMilestone);
    const update = http.expectOne(`${API_BASE_URL}/api/milestones/21/status`);
    update.flush(
      { message: 'Milestone update failed' },
      { status: 503, statusText: 'Unavailable' },
    );
    http.expectOne(`${API_BASE_URL}/api/goals`).flush([completedGoal]);
    http.expectOne(`${API_BASE_URL}/api/goals/12/milestones`).flush([completedMilestone]);
    fixture.detectChanges();

    expect(component.milestonesFor(goal.id)[0].status).toBe('COMPLETED');
    expect(component.goals()[0].progressPercentage).toBe(100);
    expect(component.actionError()).toBe('Milestone update failed');
    expect(component.updatingMilestoneId()).toBeNull();
  });

  it('updates and reverses goal completion using the persisted status and progress', () => {
    const incompleteGoal = { ...goal, status: 'NOT_STARTED' as const, progressPercentage: 0 };
    flushGoals([incompleteGoal], [[]]);

    component.updateGoalStatus(incompleteGoal, 'COMPLETED');
    component.updateGoalStatus(incompleteGoal, 'NOT_STARTED');
    const completion = http.expectOne(`${API_BASE_URL}/api/goals/12/status`);
    expect(completion.request.method).toBe('PATCH');
    expect(completion.request.body).toEqual({ status: 'COMPLETED' });
    expect(component.updatingGoalId()).toBe(goal.id);
    completion.flush({ ...incompleteGoal, status: 'COMPLETED', progressPercentage: 100 });

    http
      .expectOne(`${API_BASE_URL}/api/goals`)
      .flush([{ ...incompleteGoal, status: 'COMPLETED', progressPercentage: 100 }]);
    http.expectOne(`${API_BASE_URL}/api/goals/12/milestones`).flush([]);
    expect(component.goals()[0].status).toBe('COMPLETED');
    expect(component.goals()[0].progressPercentage).toBe(100);

    component.updateGoalStatus(component.goals()[0], 'NOT_STARTED');
    const reversal = http.expectOne(`${API_BASE_URL}/api/goals/12/status`);
    expect(reversal.request.body).toEqual({ status: 'NOT_STARTED' });
    reversal.flush({ ...incompleteGoal, status: 'NOT_STARTED', progressPercentage: 0 });

    http.expectOne(`${API_BASE_URL}/api/goals`).flush([incompleteGoal]);
    http.expectOne(`${API_BASE_URL}/api/goals/12/milestones`).flush([]);
    fixture.detectChanges();

    expect(component.goals()[0].status).toBe('NOT_STARTED');
    expect(component.goals()[0].progressPercentage).toBe(0);
    expect(component.updatingGoalId()).toBeNull();
  });

  it('restores backend goal state and shows an error when a status update fails', () => {
    flushGoals([goal], [[milestone]]);

    component.updateGoalStatus(goal, 'NOT_STARTED');
    const update = http.expectOne(`${API_BASE_URL}/api/goals/12/status`);
    update.flush(
      { message: 'Complete milestones before resetting this goal' },
      { status: 400, statusText: 'Bad Request' },
    );

    http.expectOne(`${API_BASE_URL}/api/goals`).flush([goal]);
    http.expectOne(`${API_BASE_URL}/api/goals/12/milestones`).flush([milestone]);
    fixture.detectChanges();

    expect(component.goals()[0].status).toBe(goal.status);
    expect(component.goals()[0].progressPercentage).toBe(goal.progressPercentage);
    expect(component.actionError()).toBe('Complete milestones before resetting this goal');
    expect(component.updatingGoalId()).toBeNull();
  });

  it('deletes a goal after confirmation and refreshes the list', async () => {
    flushGoals([goal], [[]]);
    const confirm = vi
      .spyOn(TestBed.inject(ConfirmationDialogService), 'confirm')
      .mockResolvedValue(true);

    await component.deleteGoal(goal);
    expect(confirm).toHaveBeenCalled();
    const deletion = http.expectOne(`${API_BASE_URL}/api/goals/12`);
    expect(deletion.request.method).toBe('DELETE');
    deletion.flush(null);

    http.expectOne(`${API_BASE_URL}/api/goals`).flush([]);
    fixture.detectChanges();
    expect(component.goals()).toEqual([]);
    expect(component.success()).toBe('Goal deleted.');
    confirm.mockRestore();
  });
});

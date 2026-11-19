import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EMPTY, finalize, forkJoin, map, of, switchMap } from 'rxjs';
import { readApiError } from '../../core/auth/auth-error';
import { RegionalDateService } from '../../core/services/regional-date.service';
import { ConfirmationDialogService } from '../../shared/confirmation-dialog/confirmation-dialog.service';
import { GoalApiService } from './data/goal-api.service';
import {
  GoalRequest,
  GoalLifecycleStatus,
  GoalResponse,
  GoalStatus,
  GoalType,
  MilestoneLifecycleStatus,
  MilestoneRequest,
  MilestoneResponse,
} from './data/goal.models';
import { MilestoneApiService } from './data/milestone-api.service';
import { GoalForm } from './goal-form/goal-form';
import { MilestoneForm } from './milestone-form/milestone-form';

interface GoalWithMilestones {
  goal: GoalResponse;
  milestones: MilestoneResponse[];
}

@Component({
  selector: 'app-goals-page',
  standalone: true,
  imports: [FormsModule, GoalForm, MilestoneForm],
  templateUrl: './goals.page.html',
})
export class GoalsPage {
  private readonly regionalDate = inject(RegionalDateService);
  private readonly goalsApi = inject(GoalApiService);
  private readonly milestonesApi = inject(MilestoneApiService);
  private readonly confirmation = inject(ConfirmationDialogService);
  private requestId = 0;

  readonly goals = signal<GoalResponse[]>([]);
  readonly milestonesByGoal = signal<Record<number, MilestoneResponse[]>>({});
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly saving = signal(false);
  readonly deletingGoalId = signal<number | null>(null);
  readonly deletingMilestoneId = signal<number | null>(null);
  readonly updatingGoalId = signal<number | null>(null);
  readonly updatingMilestoneId = signal<number | null>(null);
  readonly goalFormOpen = signal(false);
  readonly selectedGoal = signal<GoalResponse | null>(null);
  readonly goalFormError = signal<string | null>(null);
  readonly milestoneFormOpen = signal(false);
  readonly milestoneGoal = signal<GoalResponse | null>(null);
  readonly selectedMilestone = signal<MilestoneResponse | null>(null);
  readonly milestoneFormError = signal<string | null>(null);
  readonly searchText = signal('');
  readonly statusFilter = signal<GoalLifecycleStatus | ''>('');
  readonly typeFilter = signal<GoalType | ''>('');
  readonly visibleGoals = computed(() => {
    const search = this.searchText().trim().toLowerCase();
    return this.goals().filter((goal) => {
      const matchesSearch =
        !search ||
        goal.title.toLowerCase().includes(search) ||
        (goal.description ?? '').toLowerCase().includes(search);
      return (
        matchesSearch &&
        (!this.statusFilter() || goal.status === this.statusFilter()) &&
        (!this.typeFilter() || goal.goalType === this.typeFilter())
      );
    });
  });

  constructor() {
    this.loadGoals();
  }

  loadGoals(): void {
    const requestId = ++this.requestId;
    this.loading.set(true);
    this.error.set(null);
    this.goalsApi
      .list()
      .pipe(
        switchMap((goals) =>
          goals.length
            ? forkJoin(
                goals.map((goal) =>
                  this.milestonesApi
                    .listForGoal(goal.id)
                    .pipe(map((milestones) => ({ goal, milestones }) satisfies GoalWithMilestones)),
                ),
              )
            : of([] as GoalWithMilestones[]),
        ),
        finalize(() => {
          if (requestId === this.requestId) this.loading.set(false);
        }),
      )
      .subscribe({
        next: (items) => {
          if (requestId !== this.requestId) return;
          this.goals.set(items.map(({ goal }) => goal));
          this.milestonesByGoal.set(
            Object.fromEntries(items.map(({ goal, milestones }) => [goal.id, milestones])),
          );
        },
        error: (error: unknown) => {
          if (requestId !== this.requestId) return;
          this.error.set(
            readApiError(error, 'Could not load goals and milestones. Please try again.'),
          );
        },
      });
  }

  beginCreateGoal(): void {
    this.selectedGoal.set(null);
    this.goalFormError.set(null);
    this.goalFormOpen.set(true);
  }

  beginEditGoal(goal: GoalResponse): void {
    this.selectedGoal.set(goal);
    this.goalFormError.set(null);
    this.goalFormOpen.set(true);
  }

  saveGoal(event: { id: number | null; request: GoalRequest }): void {
    this.saving.set(true);
    this.goalFormError.set(null);
    const operation =
      event.id === null
        ? this.goalsApi.create(event.request)
        : this.goalsApi.update(event.id, event.request);
    operation.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.goalFormOpen.set(false);
        this.success.set(event.id === null ? 'Goal created.' : 'Goal updated.');
        this.loadGoals();
      },
      error: (error: unknown) => {
        this.goalFormError.set(
          readApiError(
            error,
            'Could not save this goal. Check that its dates contain all milestone dates.',
          ),
        );
      },
    });
  }

  async deleteGoal(goal: GoalResponse): Promise<void> {
    if (!(await this.confirmation.confirm({
      title: 'Delete this goal?',
      message: `Delete "${goal.title}" and its milestones? Tasks linked to them will be unlinked.`,
      confirmLabel: 'Yes, Delete',
    }))) return;
    this.deletingGoalId.set(goal.id);
    this.actionError.set(null);
    this.goalsApi
      .delete(goal.id)
      .pipe(finalize(() => this.deletingGoalId.set(null)))
      .subscribe({
        next: () => {
          this.success.set('Goal deleted.');
          this.loadGoals();
        },
        error: (error: unknown) => {
          this.actionError.set(readApiError(error, 'Could not delete this goal.'));
        },
      });
  }

  updateGoalStatus(goal: GoalResponse, status: GoalStatus): void {
    if (status === goal.status || this.updatingGoalId() !== null) return;
    this.updatingGoalId.set(goal.id);
    this.actionError.set(null);
    this.goalsApi
      .updateStatus(goal.id, status)
      .pipe(finalize(() => this.updatingGoalId.set(null)))
      .subscribe({
        next: () => {
          this.success.set(`Goal status changed to ${this.statusLabel(status)}.`);
          this.loadGoals();
        },
        error: (error: unknown) => {
          this.actionError.set(readApiError(error, 'Could not update goal status.'));
          this.loadGoals();
        },
      });
  }

  beginCreateMilestone(goal: GoalResponse): void {
    this.milestoneGoal.set(goal);
    this.selectedMilestone.set(null);
    this.milestoneFormError.set(null);
    this.milestoneFormOpen.set(true);
  }

  beginEditMilestone(goal: GoalResponse, milestone: MilestoneResponse): void {
    this.milestoneGoal.set(goal);
    this.selectedMilestone.set(milestone);
    this.milestoneFormError.set(null);
    this.milestoneFormOpen.set(true);
  }

  saveMilestone(event: { id: number | null; request: MilestoneRequest }): void {
    const goal = this.milestoneGoal();
    if (!goal) return;
    this.saving.set(true);
    this.milestoneFormError.set(null);
    const operation =
      event.id === null
        ? this.milestonesApi.create(goal.id, event.request)
        : this.milestonesApi.update(event.id, event.request);
    operation.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.milestoneFormOpen.set(false);
        this.success.set(event.id === null ? 'Milestone created.' : 'Milestone updated.');
        this.loadGoals();
      },
      error: (error: unknown) => {
        this.milestoneFormError.set(
          readApiError(
            error,
            'Could not save this milestone. Its target date must be within the goal date range.',
          ),
        );
      },
    });
  }

  toggleMilestoneCompletion(milestone: MilestoneResponse): void {
    if (this.updatingMilestoneId() !== null) return;
    const status = milestone.status === 'COMPLETED' ? 'NOT_STARTED' : 'COMPLETED';
    this.updatingMilestoneId.set(milestone.id);
    this.actionError.set(null);
    this.milestonesApi
      .updateStatus(milestone.id, status)
      .pipe(finalize(() => this.updatingMilestoneId.set(null)))
      .subscribe({
        next: () => {
          this.success.set(
            status === 'COMPLETED' ? 'Milestone completed.' : 'Milestone marked not started.',
          );
          this.loadGoals();
        },
        error: (error: unknown) => {
          this.actionError.set(readApiError(error, 'Could not update this milestone.'));
          this.loadGoals();
        },
      });
  }

  async deleteMilestone(milestone: MilestoneResponse): Promise<void> {
    if (!(await this.confirmation.confirm({
      title: 'Delete this milestone?',
      message: `Delete milestone "${milestone.title}"?`,
      confirmLabel: 'Yes, Delete',
    }))) return;
    this.deletingMilestoneId.set(milestone.id);
    this.actionError.set(null);
    this.milestonesApi
      .delete(milestone.id)
      .pipe(finalize(() => this.deletingMilestoneId.set(null)))
      .subscribe({
        next: () => {
          this.success.set('Milestone deleted.');
          this.loadGoals();
        },
        error: (error: unknown) => {
          this.actionError.set(readApiError(error, 'Could not delete this milestone.'));
        },
      });
  }

  milestonesFor(goalId: number): MilestoneResponse[] {
    return this.milestonesByGoal()[goalId] ?? [];
  }

  updateStatusFilter(value: string): void {
    if (['', 'NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'OVERDUE', 'CANCELLED'].includes(value)) {
      this.statusFilter.set(value as GoalLifecycleStatus | '');
    }
  }

  updateTypeFilter(value: string): void {
    if (['', 'WEEKLY', 'MONTHLY', 'YEARLY'].includes(value)) {
      this.typeFilter.set(value as GoalType | '');
    }
  }

  statusLabel(status: GoalLifecycleStatus | MilestoneLifecycleStatus): string {
    return {
      NOT_STARTED: 'Not started',
      IN_PROGRESS: 'In progress',
      COMPLETED: 'Completed',
      OVERDUE: 'Overdue',
      CANCELLED: 'Cancelled',
    }[status];
  }

  statusClass(status: GoalLifecycleStatus | MilestoneLifecycleStatus): string {
    return {
      NOT_STARTED: 'bg-ls-bg text-ls-text-muted',
      IN_PROGRESS: 'bg-purple/15 text-purple',
      COMPLETED: 'bg-mint/35 text-ls-text',
      OVERDUE: 'bg-burgundy text-white',
      CANCELLED: 'bg-ls-surface-2 text-ls-text',
    }[status];
  }

  goalTypeLabel(type: GoalType): string {
    return { WEEKLY: 'Weekly', MONTHLY: 'Monthly', YEARLY: 'Yearly' }[type];
  }

  formatDate(value: string): string {
    return this.regionalDate.formatDateOnly(value, { dateStyle: 'medium' });
  }

  clearSuccess(): void {
    this.success.set(null);
  }
}

import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { readApiError } from '../../core/auth/auth-error';
import { RegionalDateService } from '../../core/services/regional-date.service';
import { CalendarTask, ScheduleSlotRequest, ScheduleSlotResponse } from '../calendar/data/calendar.models';
import { ScheduleApiService } from '../calendar/data/schedule-api.service';
import { ScheduleForm } from '../calendar/schedule-form/schedule-form';
import { TaskApiService } from '../tasks/data/task-api.service';
import { TaskResponse } from '../tasks/data/task.models';
import { DailyPlanPanel } from './components/daily-plan-panel';

function localDate(date = new Date()): string {
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

@Component({
  selector: 'app-planner-page',
  standalone: true,
  imports: [CommonModule, RouterLink, ScheduleForm, DailyPlanPanel],
  templateUrl: './planner.page.html',
})
export class PlannerPage {
  private readonly taskApi = inject(TaskApiService);
  private readonly scheduleApi = inject(ScheduleApiService);
  private readonly regionalDate = inject(RegionalDateService);

  readonly today = localDate();
  readonly tasks = signal<TaskResponse[]>([]);
  readonly schedules = signal<ScheduleSlotResponse[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly message = signal<string | null>(null);
  readonly scheduleFormOpen = signal(false);
  readonly editingSchedule = signal<ScheduleSlotResponse | null>(null);
  readonly scheduleTaskId = signal<number | null>(null);
  readonly scheduleFormError = signal<string | null>(null);
  readonly savingSchedule = signal(false);

  readonly todayTasks = computed(() => {
    const scheduledTaskIds = new Set(
      this.schedules()
        .filter((slot) => slot.startDateTime.slice(0, 10) === this.today && slot.taskId !== null)
        .map((slot) => slot.taskId),
    );
    return this.sortedTasks(
      this.tasks().filter(
        (task) => task.dueDate?.slice(0, 10) === this.today || scheduledTaskIds.has(task.id),
      ),
    );
  });
  readonly pendingTasks = computed(() =>
    this.sortedTasks(this.tasks().filter((task) => this.isPending(task))),
  );
  readonly overdueTasks = computed(() =>
    this.sortedTasks(
      this.pendingTasks().filter((task) => task.status === 'OVERDUE'),
    ),
  );
  readonly upcomingTasks = computed(() =>
    this.sortedTasks(
      this.pendingTasks().filter(
        (task) => task.dueDate !== null && task.dueDate.slice(0, 10) > this.today,
      ),
    ),
  );
  readonly todaySchedules = computed(() =>
    this.schedules()
      .filter((slot) => slot.startDateTime.slice(0, 10) === this.today)
      .sort((a, b) => a.startDateTime.localeCompare(b.startDateTime)),
  );
  readonly dayProgress = computed(() => {
    const tasks = this.todayTasks();
    const completed = tasks.filter((task) => task.status === 'COMPLETED').length;
    return { completed, total: tasks.length, percentage: tasks.length ? Math.round((completed / tasks.length) * 100) : 0 };
  });
  readonly scheduleTasks = computed<CalendarTask[]>(() =>
    this.tasks()
      .filter((task) =>
        this.isPending(task) || task.id === this.editingSchedule()?.taskId,
      )
      .filter((task) =>
        !this.schedules().some((slot) =>
          slot.taskId === task.id && slot.id !== this.editingSchedule()?.id,
        ),
      )
      .map((task) => ({
        id: task.id,
        title: task.title,
        status: task.status,
        dueDate: task.dueDate,
        startDate: task.startDate,
      })),
  );

  constructor() {
    this.loadPlanner();
  }

  loadPlanner(): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      tasks: this.taskApi.listAll(),
      schedules: this.scheduleApi.listAll(),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ tasks, schedules }) => {
          this.tasks.set(tasks);
          this.schedules.set(schedules);
        },
        error: (error: unknown) => {
          this.error.set(readApiError(error, 'Could not load your plan. Please try again.'));
        },
      });
  }

  completeTask(task: TaskResponse): void {
    this.taskApi.complete(task.id).subscribe({
      next: () => {
        this.message.set(`“${task.title}” marked complete.`);
        this.loadPlanner();
      },
      error: (error: unknown) => {
        this.error.set(readApiError(error, 'Could not update this task.'));
      },
    });
  }

  beginCreateSchedule(taskId: number | null = null): void {
    this.editingSchedule.set(null);
    this.scheduleTaskId.set(taskId);
    this.scheduleFormError.set(null);
    this.scheduleFormOpen.set(true);
  }

  editSchedule(slot: ScheduleSlotResponse): void {
    this.editingSchedule.set(slot);
    this.scheduleTaskId.set(slot.taskId);
    this.scheduleFormError.set(null);
    this.scheduleFormOpen.set(true);
  }

  saveSchedule(event: { id: number | null; request: ScheduleSlotRequest }): void {
    this.savingSchedule.set(true);
    this.scheduleFormError.set(null);
    const save = () => {
      const request = event.id === null
        ? this.scheduleApi.create(event.request)
        : this.scheduleApi.update(event.id, event.request);
      request.pipe(finalize(() => this.savingSchedule.set(false))).subscribe({
        next: () => {
          this.scheduleFormOpen.set(false);
          this.message.set(event.id === null ? 'Task added to your schedule.' : 'Schedule updated.');
          this.loadPlanner();
        },
        error: (error: unknown) => {
          this.scheduleFormError.set(readApiError(error, 'Could not save this schedule entry.'));
        },
      });
    };
    if (event.id !== null) {
      save();
      return;
    }
    this.scheduleApi.detectConflicts({
      startDateTime: event.request.startDateTime,
      endDateTime: event.request.endDateTime,
      taskId: event.request.taskId,
    }).subscribe({
      next: (result) => {
        if (result.hasConflicts) {
          this.savingSchedule.set(false);
          this.scheduleFormError.set('Choose a time that does not overlap another calendar item.');
          return;
        }
        save();
      },
      error: (error: unknown) => {
        this.savingSchedule.set(false);
        this.scheduleFormError.set(readApiError(error, 'Could not check the schedule for conflicts.'));
      },
    });
  }

  isPending(task: TaskResponse): boolean {
    return task.status === 'TODO' || task.status === 'IN_PROGRESS' || task.status === 'OVERDUE';
  }

  priorityClass(priority: TaskResponse['priority']): string {
    return {
      URGENT: 'bg-burgundy text-white',
      HIGH: 'bg-tangerine text-plum',
      MEDIUM: 'bg-rose text-plum',
      LOW: 'bg-ls-bg text-ls-text-muted',
    }[priority];
  }

  formatDueDate(value: string | null): string {
    if (!value) return 'No deadline';
    return this.regionalDate.formatDateTime(value, {
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
    });
  }

  formatTime(value: string): string {
    return this.regionalDate.formatTime(value, { hour: 'numeric', minute: '2-digit' });
  }

  private sortedTasks(tasks: TaskResponse[]): TaskResponse[] {
    const rank: Record<TaskResponse['priority'], number> = {
      URGENT: 0,
      HIGH: 1,
      MEDIUM: 2,
      LOW: 3,
    };
    return [...tasks].sort((a, b) =>
      rank[a.priority] - rank[b.priority]
      || (a.dueDate ?? '9999').localeCompare(b.dueDate ?? '9999')
      || a.title.localeCompare(b.title),
    );
  }
}

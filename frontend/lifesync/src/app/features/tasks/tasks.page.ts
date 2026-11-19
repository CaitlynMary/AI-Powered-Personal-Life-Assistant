import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom, finalize, map, Observable, tap, throwError } from 'rxjs';
import { readApiError } from '../../core/auth/auth-error';
import { RegionalDateService } from '../../core/services/regional-date.service';
import { ConfirmationDialogService } from '../../shared/confirmation-dialog/confirmation-dialog.service';
import { CategoryManager } from './categories/category-manager';
import { CategoryApiService } from './data/category-api.service';
import { CategoryResponse, TaskPageResponse, TaskPriority, TaskQuery, TaskRequest, TaskResponse, TaskStatus, TaskUpdateStatus } from './data/task.models';
import { TaskApiService } from './data/task-api.service';
import { TaskForm } from './task-form/task-form';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-tasks-page',
  standalone: true,
  imports: [FormsModule, CategoryManager, TaskForm],
  templateUrl: './tasks.page.html',
})
export class TasksPage {
  private readonly tasksApi = inject(TaskApiService);
  private readonly categoriesApi = inject(CategoryApiService);
  private readonly confirmation = inject(ConfirmationDialogService);
  private readonly regionalDate = inject(RegionalDateService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly tasks = signal<TaskResponse[]>([]);
  readonly categories = signal<CategoryResponse[]>([]);
  readonly loading = signal(true);
  readonly categoriesLoading = signal(true);
  readonly saving = signal(false);
  readonly deletingTaskId = signal<number | null>(null);
  readonly confirmingDeleteTaskId = signal<number | null>(null);
  readonly updatingTaskId = signal<number | null>(null);
  readonly error = signal<string | null>(null);
  readonly categoriesError = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly selectedTask = signal<TaskResponse | null>(null);
  readonly formOpen = signal(false);
  readonly formError = signal<string | null>(null);
  readonly statusFilter = signal<TaskStatus | ''>('');
  readonly priorityFilter = signal<TaskPriority | ''>('');
  readonly categoryFilter = signal<number | ''>('');
  readonly searchText = signal('');
  readonly submittedSearch = signal('');
  readonly sortBy = signal<TaskQuery['sortBy']>('createdAt');
  readonly direction = signal<TaskQuery['direction']>('DESC');
  private taskRequestId = 0;

  constructor() {
    this.loadCategories();
    this.loadTasks();
    if (this.route.snapshot.queryParamMap.get('openCreateTask') === 'true') {
      this.beginCreate();
      void this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { openCreateTask: null },
        queryParamsHandling: 'merge',
        replaceUrl: true,
      });
    }
  }

  loadCategories(): void {
    this.categoriesLoading.set(true);
    this.categoriesError.set(null);
    this.categoriesApi.list().subscribe({
      next: (categories) => {
        this.categories.set(categories);
        this.categoriesLoading.set(false);
      },
      error: (error: unknown) => {
        this.categoriesError.set(readApiError(error, 'Could not load categories.'));
        this.categoriesLoading.set(false);
      },
    });
  }

  loadTasks(): void {
    const requestId = ++this.taskRequestId;
    this.loading.set(true);
    this.error.set(null);
    this.tasksApi
      .list({
        page: this.page(),
        size: PAGE_SIZE,
        status: this.statusFilter(),
        priority: this.priorityFilter(),
        categoryId: this.categoryFilter(),
        search: this.submittedSearch(),
        sortBy: this.sortBy(),
        direction: this.direction(),
      })
      .pipe(finalize(() => {
        if (requestId === this.taskRequestId) this.loading.set(false);
      }))
      .subscribe({
        next: (response: TaskPageResponse) => {
          if (requestId !== this.taskRequestId) return;
          this.tasks.set(response.content);
          this.page.set(response.page);
          this.totalPages.set(response.totalPages);
          this.totalElements.set(response.totalElements);
        },
        error: (error: unknown) => {
          if (requestId !== this.taskRequestId) return;
          this.error.set(readApiError(error, 'Could not load tasks. Please try again.'));
        },
      });
  }

  updateStatusFilter(value: string): void {
    if (['', 'TODO', 'IN_PROGRESS', 'COMPLETED', 'OVERDUE', 'MISSED', 'CANCELLED'].includes(value)) {
      this.statusFilter.set(value as TaskStatus | '');
      this.applyFilters();
    }
  }

  updatePriorityFilter(value: string): void {
    if (['', 'LOW', 'MEDIUM', 'HIGH', 'URGENT'].includes(value)) {
      this.priorityFilter.set(value as TaskPriority | '');
      this.applyFilters();
    }
  }

  updateCategoryFilter(value: number | ''): void {
    this.categoryFilter.set(value);
    this.applyFilters();
  }

  updateSortBy(value: string): void {
    const sortOptions: TaskQuery['sortBy'][] = [
      'createdAt',
      'updatedAt',
      'title',
      'dueDate',
      'priority',
      'status',
      'startDate',
    ];
    if (sortOptions.includes(value as TaskQuery['sortBy'])) {
      this.sortBy.set(value as TaskQuery['sortBy']);
      this.applyFilters();
    }
  }

  applyFilters(): void {
    this.submittedSearch.set(this.searchText().trim());
    this.page.set(0);
    this.loadTasks();
  }

  changePage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.page.set(page);
    this.loadTasks();
  }

  beginCreate(): void {
    this.selectedTask.set(null);
    this.formError.set(null);
    this.formOpen.set(true);
  }

  beginEdit(task: TaskResponse): void {
    this.selectedTask.set(task);
    this.formError.set(null);
    this.formOpen.set(true);
  }

  cancelForm(): void {
    this.formOpen.set(false);
    this.formError.set(null);
  }

  saveTask(event: { id: number | null; request: TaskRequest }): void {
    this.saving.set(true);
    this.formError.set(null);
    const operation = event.id === null
      ? this.tasksApi.create(event.request)
      : this.tasksApi.update(event.id, event.request);

    operation.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.formOpen.set(false);
        this.success.set(event.id === null ? 'Task created successfully.' : 'Task updated successfully.');
        this.loadTasks();
      },
      error: (error: unknown) => {
        this.formError.set(readApiError(error, 'Could not save this task. Please try again.'));
      },
    });
  }

  toggleCompletion(task: TaskResponse): void {
    if (task.status === 'CANCELLED' || task.status === 'MISSED') return;
    if (task.status === 'COMPLETED') this.setTaskStatus(task, 'TODO', 'Task reopened.');
    else this.completeTask(task);
  }

  startTask(task: TaskResponse): void {
    this.setTaskStatus(task, 'IN_PROGRESS', 'Task started.');
  }

  completeTask(task: TaskResponse): void {
    if (this.updatingTaskId() !== null) return;
    this.updatingTaskId.set(task.id);
    this.actionError.set(null);
    this.tasksApi.complete(task.id)
      .pipe(finalize(() => this.updatingTaskId.set(null)))
      .subscribe({
        next: () => {
          this.success.set('Task completed.');
          this.loadTasks();
        },
        error: (error: unknown) => this.actionError.set(readApiError(error, 'Could not complete this task.')),
      });
  }

  async markTaskMissed(task: TaskResponse): Promise<void> {
    await this.confirmation.confirm({
      title: 'Mark this task as missed?',
      message: 'This will preserve the task in your history and remove it from active work.',
      confirmLabel: 'Mark as Missed',
    }, () => this.confirmTaskStatus(task, 'MISSED', 'Task marked as missed.'));
  }

  async cancelTask(task: TaskResponse): Promise<void> {
    await this.confirmation.confirm({
      title: 'Cancel this task?',
      message: 'Cancelling moves this task out of active work while keeping it in your history.',
      confirmLabel: 'Cancel Task',
    }, () => this.confirmTaskStatus(task, 'CANCELLED', 'Task cancelled.'));
  }

  restoreTask(task: TaskResponse): void {
    this.setTaskStatus(task, 'TODO', 'Task restored.');
  }

  private setTaskStatus(task: TaskResponse, status: TaskUpdateStatus, success: string): void {
    this.updateTaskStatus(task, status, success).subscribe({
      error: (error: unknown) => this.actionError.set(readApiError(error, 'Could not update task status.')),
    });
  }

  private updateTaskStatus(
    task: TaskResponse,
    status: TaskUpdateStatus,
    success: string,
  ): Observable<void> {
    if (this.updatingTaskId() !== null) {
      return throwError(() => new Error('Another task status update is already in progress.'));
    }
    this.updatingTaskId.set(task.id);
    this.actionError.set(null);
    return this.tasksApi.setStatus(task.id, status)
      .pipe(
        tap(() => {
          this.success.set(success);
          this.loadTasks();
        }),
        map(() => undefined),
        finalize(() => this.updatingTaskId.set(null)),
      );
  }

  private async confirmTaskStatus(
    task: TaskResponse,
    status: TaskUpdateStatus,
    success: string,
  ): Promise<void> {
    try {
      await firstValueFrom(this.updateTaskStatus(task, status, success));
    } catch (error: unknown) {
      throw new Error(readApiError(error, 'Could not update task status.'));
    }
  }

  async deleteTask(task: TaskResponse): Promise<void> {
    if (this.deletingTaskId() !== null) return;
    this.deletingTaskId.set(task.id);
    this.confirmingDeleteTaskId.set(task.id);
    let confirmed = false;
    try {
      confirmed = await this.confirmation.confirm({
        title: 'Delete this task?',
        message: 'Are you sure you want to delete this task? This action cannot be undone.',
        confirmLabel: 'Yes, Delete',
      });
    } finally {
      this.confirmingDeleteTaskId.set(null);
      if (!confirmed) this.deletingTaskId.set(null);
    }
    if (!confirmed) return;
    this.actionError.set(null);
    this.tasksApi
      .delete(task.id)
      .pipe(finalize(() => this.deletingTaskId.set(null)))
      .subscribe({
        next: () => {
          this.success.set('Task deleted.');
          if (this.tasks().length === 1 && this.page() > 0) this.page.update((page) => page - 1);
          this.loadTasks();
        },
        error: (error: unknown) => {
          this.actionError.set(readApiError(error, 'Could not delete this task.'));
        },
      });
  }

  categoriesChanged(): void {
    this.loadCategories();
    this.loadTasks();
  }

  clearSuccess(): void {
    this.success.set(null);
  }

  statusLabel(status: TaskStatus): string {
    return {
      TODO: 'Pending',
      IN_PROGRESS: 'In progress',
      COMPLETED: 'Completed',
      OVERDUE: 'Overdue',
      MISSED: 'Missed',
      CANCELLED: 'Cancelled',
    }[status];
  }

  statusClass(status: TaskStatus): string {
    return {
      TODO: 'bg-ls-bg text-ls-text-muted',
      IN_PROGRESS: 'bg-powder-blue text-plum',
      COMPLETED: 'bg-aqua text-plum',
      OVERDUE: 'bg-burgundy text-white',
      MISSED: 'bg-tangerine text-plum',
      CANCELLED: 'bg-ls-surface-2 text-ls-text-muted',
    }[status];
  }

  priorityClass(priority: TaskPriority): string {
    return {
      LOW: 'bg-aqua text-plum',
      MEDIUM: 'bg-rose text-plum',
      HIGH: 'bg-tangerine text-plum',
      URGENT: 'bg-burgundy text-white',
    }[priority];
  }

  formatDateTime(value: string | null): string {
    if (!value) return 'No due date';
    return this.regionalDate.formatDateTime(value, { dateStyle: 'medium', timeStyle: 'short' });
  }
}

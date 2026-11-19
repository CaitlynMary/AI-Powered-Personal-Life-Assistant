import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { ComponentFixture } from '@angular/core/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, ParamMap, Router, convertToParamMap } from '@angular/router';
import { API_BASE_URL } from '../../core/api.config';
import { ConfirmationDialogService } from '../../shared/confirmation-dialog/confirmation-dialog.service';
import { CategoryResponse, TaskPageResponse, TaskResponse } from './data/task.models';
import { TasksPage } from './tasks.page';

const category: CategoryResponse = {
  id: 5,
  name: 'Work',
  description: null,
  color: '#81689D',
  createdAt: '2026-06-01T10:00:00',
  updatedAt: '2026-06-01T10:00:00',
};

const task: TaskResponse = {
  id: 9,
  title: 'Review project',
  description: 'Check the next milestone',
  categoryId: 5,
  categoryName: 'Work',
  goalId: null,
  milestoneId: null,
  priority: 'HIGH',
  status: 'TODO',
  dueDate: '2026-06-15T10:30:00',
  startDate: null,
  estimatedDuration: 30,
  actualDuration: null,
  completedAt: null,
  missedAt: null,
  recurring: false,
  recurrenceType: 'NONE',
  createdAt: '2026-06-01T10:00:00',
  updatedAt: '2026-06-01T10:00:00',
};

function taskPage(content: TaskResponse[]): TaskPageResponse {
  return {
    content,
    page: 0,
    size: 20,
    totalElements: content.length,
    totalPages: content.length ? 1 : 0,
    last: true,
  };
}

function taskWithStatus(status: TaskResponse['status'], id: number): TaskResponse {
  return {
    ...task,
    id,
    status,
    dueDate: status === 'OVERDUE' ? '2026-06-01T10:30:00' : task.dueDate,
  };
}

describe('TasksPage', () => {
  let fixture: ComponentFixture<TasksPage>;
  let component: TasksPage;
  let http: HttpTestingController;
  let route: { snapshot: { queryParamMap: ParamMap } };
  let routerNavigate: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    route = { snapshot: { queryParamMap: convertToParamMap({}) } };
    routerNavigate = vi.fn().mockResolvedValue(true);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: route },
        { provide: Router, useValue: { navigate: routerNavigate } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function flushInitial(tasks: TaskResponse[] = []): void {
    http.expectOne(`${API_BASE_URL}/api/categories`).flush([category]);
    http.expectOne((request) => request.url === `${API_BASE_URL}/api/tasks`).flush(taskPage(tasks));
    fixture.detectChanges();
  }

  function createPage(openCreateTask = false): void {
    route.snapshot.queryParamMap = convertToParamMap(
      openCreateTask ? { openCreateTask: 'true' } : {},
    );
    fixture = TestBed.createComponent(TasksPage);
    component = fixture.componentInstance;
  }

  it('shows loading, then an empty state when no tasks exist', () => {
    createPage();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Loading your tasks');
    expect(component.formOpen()).toBe(false);
    expect(routerNavigate).not.toHaveBeenCalled();

    flushInitial([]);

    expect(fixture.nativeElement.textContent).toContain('No tasks found');
  });

  it('renders real task details after loading', () => {
    createPage();
    flushInitial([task]);

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Review project');
    expect(text).toContain('Check the next milestone');
    expect(text).toContain('Work');
    expect(text).toContain('HIGH');
  });

  it('shows task API errors with a retry action', () => {
    createPage();
    fixture.detectChanges();
    http.expectOne(`${API_BASE_URL}/api/categories`).flush([]);
    http.expectOne((request) => request.url === `${API_BASE_URL}/api/tasks`).flush(
      { message: 'Task service unavailable' },
      { status: 503, statusText: 'Unavailable' },
    );
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Task service unavailable');
    expect(fixture.nativeElement.textContent).toContain('Retry');
  });

  it('trims submitted searches and omits empty search parameters', () => {
    createPage();
    flushInitial([task]);
    component.searchText.set('  Learn Coding  ');
    component.applyFilters();
    const searchRequest = http.expectOne(
      (request) => request.url === `${API_BASE_URL}/api/tasks`,
    );
    expect(searchRequest.request.params.get('search')).toBe('Learn Coding');
    searchRequest.flush(taskPage([task]));

    component.statusFilter.set('TODO');
    component.searchText.set('   ');
    component.applyFilters();
    const emptySearchRequest = http.expectOne(
      (request) => request.url === `${API_BASE_URL}/api/tasks`,
    );
    expect(emptySearchRequest.request.params.has('search')).toBe(false);
    expect(emptySearchRequest.request.params.get('status')).toBe('TODO');
    emptySearchRequest.flush(taskPage([task]));
  });

  it('marks a task completed and reloads the list', () => {
    createPage();
    flushInitial([task]);

    component.toggleCompletion(task);
    const completeRequest = http.expectOne(`${API_BASE_URL}/api/tasks/9/complete`);
    expect(completeRequest.request.method).toBe('PATCH');
    completeRequest.flush({ ...task, status: 'COMPLETED' });

    http.expectOne((request) => request.url === `${API_BASE_URL}/api/tasks`).flush(
      taskPage([{ ...task, status: 'COMPLETED' }]),
    );
    fixture.detectChanges();

    expect(component.tasks()[0].status).toBe('COMPLETED');
    expect(component.success()).toBe('Task completed.');
  });

  it('lets the checkbox reopen a completed task without confirmation', () => {
    const completedTask = {
      ...task,
      status: 'COMPLETED' as const,
      completedAt: '2026-06-15T10:30:00',
    };
    createPage();
    flushInitial([completedTask]);

    const checkbox = fixture.nativeElement.querySelector(
      'li button[aria-pressed="true"]',
    ) as HTMLButtonElement;
    expect(checkbox.disabled).toBe(false);
    checkbox.click();

    const statusRequest = http.expectOne(`${API_BASE_URL}/api/tasks/9/status`);
    expect(statusRequest.request.body).toEqual({ status: 'TODO' });
    statusRequest.flush({ ...completedTask, status: 'IN_PROGRESS', completedAt: null });
    http.expectOne((request) => request.url === `${API_BASE_URL}/api/tasks`).flush(
      taskPage([{ ...task, status: 'IN_PROGRESS' }]),
    );

    expect(component.success()).toBe('Task reopened.');
  });

  it('does not send a status update when Mark as Missed is cancelled', async () => {
    createPage();
    flushInitial([task]);
    vi.spyOn(TestBed.inject(ConfirmationDialogService), 'confirm').mockResolvedValue(false);

    await component.markTaskMissed(task);

    http.expectNone(`${API_BASE_URL}/api/tasks/9/status`);
    expect(component.tasks()).toEqual([task]);
  });

  it('marks a task missed only after confirmation and refreshes the list', async () => {
    createPage();
    flushInitial([task]);
    vi.spyOn(TestBed.inject(ConfirmationDialogService), 'confirm').mockImplementation(
      async (_options, action) => {
        await action?.();
        return true;
      },
    );

    const marking = component.markTaskMissed(task);
    const statusRequest = http.expectOne(`${API_BASE_URL}/api/tasks/9/status`);
    expect(statusRequest.request.method).toBe('PATCH');
    expect(statusRequest.request.body).toEqual({ status: 'MISSED' });
    statusRequest.flush({ ...task, status: 'MISSED', missedAt: '2026-06-15T10:30:00' });
    http.expectOne((request) => request.url === `${API_BASE_URL}/api/tasks`).flush(
      taskPage([taskWithStatus('MISSED', task.id)]),
    );
    await marking;

    expect(component.tasks()[0].status).toBe('MISSED');
    expect(component.success()).toBe('Task marked as missed.');
  });

  it('keeps task state and the list unchanged when marking as missed fails', async () => {
    createPage();
    flushInitial([task]);
    vi.spyOn(TestBed.inject(ConfirmationDialogService), 'confirm').mockImplementation(
      async (_options, action) => {
        try {
          await action?.();
          return true;
        } catch {
          return false;
        }
      },
    );

    const marking = component.markTaskMissed(task);
    http.expectOne(`${API_BASE_URL}/api/tasks/9/status`).flush(
      { message: 'Task status update failed' },
      { status: 500, statusText: 'Server Error' },
    );
    await marking;

    expect(component.tasks()).toEqual([task]);
    expect(component.success()).toBeNull();
    http.expectNone((request) => request.url === `${API_BASE_URL}/api/tasks`);
  });

  it('does not send a cancellation update when the task-card dialog is cancelled', async () => {
    createPage();
    flushInitial([task]);
    vi.spyOn(TestBed.inject(ConfirmationDialogService), 'confirm').mockResolvedValue(false);

    await component.cancelTask(task);

    http.expectNone(`${API_BASE_URL}/api/tasks/9/status`);
    expect(component.tasks()).toEqual([task]);
  });

  it('confirms task cancellation separately and changes it to Cancelled', async () => {
    createPage();
    flushInitial([task]);
    const confirm = vi
      .spyOn(TestBed.inject(ConfirmationDialogService), 'confirm')
      .mockImplementation(async (_options, action) => {
        await action?.();
        return true;
      });

    const cancelling = component.cancelTask(task);
    expect(confirm).toHaveBeenCalledWith(
      {
        title: 'Cancel this task?',
        message: 'Cancelling moves this task out of active work while keeping it in your history.',
        confirmLabel: 'Cancel Task',
      },
      expect.any(Function),
    );
    const statusRequest = http.expectOne(`${API_BASE_URL}/api/tasks/9/status`);
    expect(statusRequest.request.body).toEqual({ status: 'CANCELLED' });
    statusRequest.flush({ ...task, status: 'CANCELLED' });
    http.expectOne((request) => request.url === `${API_BASE_URL}/api/tasks`).flush(
      taskPage([taskWithStatus('CANCELLED', task.id)]),
    );
    await cancelling;

    expect(component.tasks()[0].status).toBe('CANCELLED');
    expect(component.success()).toBe('Task cancelled.');
  });

  it('renders only the actions appropriate to each task lifecycle state', () => {
    createPage();
    const tasks = [
      taskWithStatus('TODO', 1),
      taskWithStatus('IN_PROGRESS', 2),
      taskWithStatus('OVERDUE', 3),
      taskWithStatus('COMPLETED', 4),
      taskWithStatus('MISSED', 5),
      taskWithStatus('CANCELLED', 6),
    ];
    flushInitial(tasks);

    const cards = Array.from(fixture.nativeElement.querySelectorAll('li')) as HTMLElement[];
    const labels = (card: HTMLElement) =>
      Array.from(card.querySelectorAll('button')).map((button) => button.textContent?.trim());
    const overdueActions = labels(cards[2]);

    expect(labels(cards[0])).toContain('Start');
    expect(labels(cards[0])).not.toContain('Mark as missed');
    expect(labels(cards[1]).filter((label) => label === 'Mark as missed')).toHaveLength(1);
    expect(overdueActions.filter((label) => label === 'Mark as missed')).toHaveLength(1);
    expect(overdueActions).toContain('Reschedule');
    expect(labels(cards[3])).toContain('Reopen');
    expect(labels(cards[3])).not.toContain('Edit');
    expect(labels(cards[4])).toContain('Restore');
    expect(labels(cards[4])).toContain('Reschedule');
    expect(labels(cards[4])).not.toContain('Edit');
    expect(labels(cards[5])).toContain('Restore');
    expect(labels(cards[5])).not.toContain('Edit');
  });

  it('deletes only after confirmation and refreshes the list', async () => {
    createPage();
    flushInitial([task]);
    const confirm = vi
      .spyOn(TestBed.inject(ConfirmationDialogService), 'confirm')
      .mockResolvedValue(true);

    await component.deleteTask(task);
    expect(confirm).toHaveBeenCalledWith({
      title: 'Delete this task?',
      message: 'Are you sure you want to delete this task? This action cannot be undone.',
      confirmLabel: 'Yes, Delete',
    });
    const deleteRequest = http.expectOne(`${API_BASE_URL}/api/tasks/9`);
    expect(deleteRequest.request.method).toBe('DELETE');
    deleteRequest.flush(null);

    http.expectOne((request) => request.url === `${API_BASE_URL}/api/tasks`).flush(taskPage([]));
    fixture.detectChanges();

    expect(component.tasks()).toEqual([]);
    expect(component.success()).toBe('Task deleted.');
    confirm.mockRestore();
  });

  it('does not send a delete request when the confirmation is cancelled', async () => {
    createPage();
    flushInitial([task]);
    vi.spyOn(TestBed.inject(ConfirmationDialogService), 'confirm').mockResolvedValue(false);

    await component.deleteTask(task);

    http.expectNone(`${API_BASE_URL}/api/tasks/9`);
    expect(component.tasks()).toEqual([task]);
    expect(component.deletingTaskId()).toBeNull();
  });

  it('prevents a second delete while the confirmation is pending', async () => {
    createPage();
    flushInitial([task]);
    let resolveConfirmation: (confirmed: boolean) => void = () => {};
    const confirm = vi
      .spyOn(TestBed.inject(ConfirmationDialogService), 'confirm')
      .mockImplementation(() => new Promise<boolean>((resolve) => {
        resolveConfirmation = resolve;
      }));

    const firstDelete = component.deleteTask(task);
    await Promise.resolve();
    await component.deleteTask(task);

    expect(confirm).toHaveBeenCalledTimes(1);
    expect(component.confirmingDeleteTaskId()).toBe(task.id);
    resolveConfirmation(false);
    await firstDelete;

    http.expectNone(`${API_BASE_URL}/api/tasks/9`);
    expect(component.deletingTaskId()).toBeNull();
  });

  it('shows the specific server message when task deletion is blocked', async () => {
    createPage();
    flushInitial([task]);
    vi.spyOn(TestBed.inject(ConfirmationDialogService), 'confirm').mockResolvedValue(true);

    const deleting = component.deleteTask(task);
    await Promise.resolve();
    http.expectOne(`${API_BASE_URL}/api/tasks/9`).flush(
      { message: 'This task is linked to a planner schedule. Remove or unlink the schedule entry before deleting the task.' },
      { status: 409, statusText: 'Conflict' },
    );
    await deleting;

    expect(component.actionError()).toBe(
      'This task is linked to a planner schedule. Remove or unlink the schedule entry before deleting the task.',
    );
    expect(component.tasks()).toEqual([task]);
    http.expectNone((request) => request.url === `${API_BASE_URL}/api/tasks`);
  });

  it('opens the existing create form for a dashboard request and clears the query parameter', () => {
    createPage(true);

    expect(component.formOpen()).toBe(true);
    expect(routerNavigate).toHaveBeenCalledWith([], {
      relativeTo: route,
      queryParams: { openCreateTask: null },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });

    flushInitial([]);

    expect(fixture.nativeElement.querySelector('[role="dialog"]')).not.toBeNull();
  });

  it('creates one task through the existing form and refreshes the list', () => {
    createPage(true);
    flushInitial([]);

    const titleInput = fixture.nativeElement.querySelector(
      'app-task-form input[formControlName="title"]',
    ) as HTMLInputElement;
    titleInput.value = 'New dashboard task';
    titleInput.dispatchEvent(new Event('input', { bubbles: true }));
    fixture.detectChanges();
    fixture.nativeElement.querySelector('app-task-form button[type="submit"]').click();

    const createRequest = http.expectOne({
      method: 'POST',
      url: `${API_BASE_URL}/api/tasks`,
    });
    expect(createRequest.request.body.title).toBe('New dashboard task');
    createRequest.flush({ ...task, id: 11, title: 'New dashboard task' });

    http.expectOne((request) => request.url === `${API_BASE_URL}/api/tasks`).flush(
      taskPage([{ ...task, id: 11, title: 'New dashboard task' }]),
    );
    fixture.detectChanges();

    expect(component.formOpen()).toBe(false);
    expect(component.tasks().map((item) => item.title)).toEqual(['New dashboard task']);
    expect(component.success()).toBe('Task created successfully.');
    http.expectNone({ method: 'POST', url: `${API_BASE_URL}/api/tasks` });
  });

  it('keeps the Tasks page create button working without navigation state', () => {
    createPage();
    flushInitial([]);

    const createButton = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((button) => button.textContent?.includes('Create task'));
    expect(createButton).toBeDefined();
    if (!createButton) return;
    expect(createButton.type).toBe('button');
    createButton.click();
    fixture.detectChanges();

    expect(component.formOpen()).toBe(true);
    expect(routerNavigate).not.toHaveBeenCalled();
  });
});

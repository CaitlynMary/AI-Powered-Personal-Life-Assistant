import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { API_BASE_URL } from '../../core/api.config';
import { ScheduleSlotResponse } from '../calendar/data/calendar.models';
import { TaskResponse } from '../tasks/data/task.models';
import { PlannerPage } from './planner.page';

function localDate(offset = 0): string {
  const date = new Date();
  date.setDate(date.getDate() + offset);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function task(
  id: number,
  title: string,
  priority: TaskResponse['priority'],
  dueDate: string | null,
  status: TaskResponse['status'] = 'TODO',
): TaskResponse {
  return {
    id,
    title,
    description: null,
    categoryId: null,
    categoryName: null,
    goalId: null,
    milestoneId: null,
    priority,
    status,
    dueDate,
    startDate: null,
    estimatedDuration: 60,
    actualDuration: null,
    completedAt: status === 'COMPLETED' ? `${localDate()}T12:00:00` : null,
    missedAt: null,
    recurring: false,
    recurrenceType: 'NONE',
    createdAt: `${localDate()}T08:00:00`,
    updatedAt: `${localDate()}T08:00:00`,
  };
}

function schedule(taskId: number, date: string): ScheduleSlotResponse {
  return {
    id: 8,
    title: 'Focus block',
    description: null,
    startDateTime: `${date}T10:00:00`,
    endDateTime: `${date}T11:00:00`,
    taskId,
    taskTitle: 'High priority work',
    createdAt: `${date}T08:00:00`,
    updatedAt: `${date}T08:00:00`,
  };
}

describe('PlannerPage', () => {
  let fixture: ComponentFixture<PlannerPage>;
  let component: PlannerPage;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    fixture = TestBed.createComponent(PlannerPage);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify({ ignoreCancelled: true }));

  function flushPlanner(tasks: TaskResponse[], schedules: ScheduleSlotResponse[]): void {
    const taskRequest = http.expectOne(
      (request) =>
        request.url === `${API_BASE_URL}/api/tasks` &&
        request.params.get('page') === '0' &&
        request.params.get('size') === '100',
    );
    taskRequest.flush({
      content: tasks,
      page: 0,
      size: 100,
      totalElements: tasks.length,
      totalPages: tasks.length ? 1 : 0,
      last: true,
    });
    http
      .expectOne(
        (request) =>
          request.url === `${API_BASE_URL}/api/schedule` &&
          request.params.get('page') === '0',
      )
      .flush({
        content: schedules,
        page: 0,
        size: 100,
        totalElements: schedules.length,
        totalPages: schedules.length ? 1 : 0,
        last: true,
      });
    fixture.detectChanges();
  }

  it('prioritizes shared tasks and separates overdue, today, and upcoming work', () => {
    const tasks = [
      task(1, 'Due today', 'URGENT', `${localDate()}T17:00:00`),
      task(2, 'Time blocked', 'HIGH', null),
      task(3, 'Past deadline', 'URGENT', `${localDate(-1)}T17:00:00`, 'OVERDUE'),
      task(4, 'Future deadline', 'MEDIUM', `${localDate(1)}T09:00:00`),
      task(5, 'Already complete', 'LOW', `${localDate()}T09:00:00`, 'COMPLETED'),
    ];
    flushPlanner(tasks, [schedule(2, localDate())]);

    expect(component.todayTasks().map((item) => item.id)).toEqual([1, 2, 5]);
    expect(component.overdueTasks().map((item) => item.id)).toEqual([3]);
    expect(component.upcomingTasks().map((item) => item.id)).toEqual([4]);
    expect(component.pendingTasks().map((item) => item.id)).toEqual([3, 1, 2, 4]);
    expect(component.dayProgress()).toEqual({ completed: 1, total: 3, percentage: 33 });
    expect(fixture.nativeElement.textContent).toContain('Time blocked');
    expect(fixture.nativeElement.textContent).toContain('Future deadline');
  });

  it('completes a shared task through the task API and reloads the same record', () => {
    const item = task(1, 'Finish report', 'HIGH', `${localDate()}T15:00:00`);
    flushPlanner([item], []);
    component.completeTask(item);

    const completion = http.expectOne(`${API_BASE_URL}/api/tasks/1/complete`);
    expect(completion.request.method).toBe('PATCH');
    completion.flush({ ...item, status: 'COMPLETED' });

    flushPlanner([{ ...item, status: 'COMPLETED' }], []);
    expect(component.todayTasks()[0].status).toBe('COMPLETED');
    expect(component.dayProgress()).toEqual({ completed: 1, total: 1, percentage: 100 });
  });
});

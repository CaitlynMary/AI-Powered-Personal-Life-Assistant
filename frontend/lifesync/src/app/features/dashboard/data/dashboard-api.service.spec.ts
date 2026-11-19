import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { API_BASE_URL } from '../../../core/api.config';
import { DashboardApiService } from './dashboard-api.service';

describe('DashboardApiService', () => {
  let service: DashboardApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DashboardApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads dashboard data and maps authenticated API responses', async () => {
    const resultPromise = firstValueFrom(service.loadDashboard());

    const tasksRequest = http.expectOne((request) => request.url === `${API_BASE_URL}/api/tasks`);
    expect(tasksRequest.request.method).toBe('GET');
    expect(tasksRequest.request.params.get('page')).toBe('0');
    expect(tasksRequest.request.params.get('size')).toBe('100');
    expect(tasksRequest.request.params.get('sortBy')).toBe('dueDate');
    expect(tasksRequest.request.params.get('direction')).toBe('ASC');
    expect(tasksRequest.request.params.get('dueFrom')).toMatch(/^\d{4}-\d{2}-\d{2}T00:00:00/);
    expect(tasksRequest.request.params.get('dueTo')).toMatch(/^\d{4}-\d{2}-\d{2}T23:59:59/);

    http
      .expectOne(`${API_BASE_URL}/api/tasks/statistics`)
      .flush({
        totalTasks: 8,
        activeTasks: 5,
        pendingTasks: 5,
        inProgressTasks: 1,
        completedTasks: 2,
        overdueTasks: 0,
        missedTasks: 1,
        cancelledTasks: 1,
      });
    http.expectOne(`${API_BASE_URL}/api/goals`).flush([
      {
        id: 21,
        title: 'Learn Angular',
        goalType: 'MONTHLY',
        status: 'IN_PROGRESS',
        progressPercentage: 50,
      },
    ]);
    http
      .expectOne(`${API_BASE_URL}/api/goals/statistics`)
      .flush({ notStarted: 2, inProgress: 3 });
    http.expectOne(`${API_BASE_URL}/api/habits`).flush([
      { id: 31, name: 'Walk', active: true },
      { id: 32, name: 'Inactive habit', active: false },
    ]);
    http
      .expectOne((request) => request.url === `${API_BASE_URL}/api/reminders/upcoming`)
      .flush({
        content: [
          {
            id: 41,
            title: 'Review notes',
            reminderDateTime: '2026-11-11T15:00:00',
            status: 'PENDING',
            reminderType: 'GENERAL',
          },
        ],
        page: 0,
        size: 100,
        totalElements: 3,
        totalPages: 1,
        last: true,
      });

    tasksRequest.flush({
      content: [
        {
          id: 11,
          title: 'Review pull request',
          priority: 'URGENT',
          status: 'TODO',
          dueDate: '2026-11-11T09:30:00',
        },
      ],
      page: 0,
      size: 100,
      totalElements: 1,
      totalPages: 1,
      last: true,
    });

    http
      .expectOne(`${API_BASE_URL}/api/goals/21/milestones`)
      .flush([
        { id: 1, goalId: 21, title: 'First step', targetDate: '2026-11-10', status: 'COMPLETED' },
        { id: 2, goalId: 21, title: 'Second step', targetDate: '2026-11-15', status: 'NOT_STARTED' },
      ]);
    const logsRequest = http.expectOne(`${API_BASE_URL}/api/habits/31/logs`);
    const today = new Date();
    const localToday = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
    logsRequest.flush([
      { id: 51, habitId: 31, completionDate: localToday, completed: true },
    ]);
    http
      .expectOne(`${API_BASE_URL}/api/habits/31/streak`)
      .flush({ currentStreak: 4, longestStreak: 6, totalCompletedDays: 12 });

    const result = await resultPromise;
    expect(result.stats.map((stat) => stat.value)).toEqual(['5', '5', '1', '2', '0', '1', '5', '4 days', '3']);
    expect(result.tasks).toEqual([
      {
        id: '11',
        title: 'Review pull request',
        completed: false,
        priority: 'urgent',
        status: 'TODO',
        dueTime: expect.any(String),
      },
    ]);
    expect(result.goals).toEqual([
      {
        id: '21',
        title: 'Learn Angular',
        progress: 50,
        icon: 'calendar-days',
        status: 'IN_PROGRESS',
      },
    ]);
    expect(result.habits).toHaveLength(1);
    expect(result.habits[0].streak).toBe(4);
    expect(result.habits[0].days).toHaveLength(7);
    expect(result.habits[0].days.at(-1)?.completed).toBe(true);
    expect(result.reminders).toEqual([
      {
        id: '41',
        title: 'Review notes',
        when: expect.any(String),
        icon: 'calendar-clock',
        status: 'PENDING',
      },
    ]);
  });

  it('returns zeroed dashboard data and empty lists when the APIs return no records', async () => {
    const resultPromise = firstValueFrom(service.loadDashboard());

    http.expectOne((request) => request.url === `${API_BASE_URL}/api/tasks`).flush({
      content: [],
      page: 0,
      size: 100,
      totalElements: 0,
      totalPages: 0,
      last: true,
    });
    http.expectOne(`${API_BASE_URL}/api/tasks/statistics`).flush({
      totalTasks: 0,
      activeTasks: 0,
      pendingTasks: 0,
      inProgressTasks: 0,
      completedTasks: 0,
      overdueTasks: 0,
      missedTasks: 0,
      cancelledTasks: 0,
    });
    http.expectOne(`${API_BASE_URL}/api/goals`).flush([]);
    http
      .expectOne(`${API_BASE_URL}/api/goals/statistics`)
      .flush({ notStarted: 0, inProgress: 0 });
    http.expectOne(`${API_BASE_URL}/api/habits`).flush([]);
    http.expectOne((request) => request.url === `${API_BASE_URL}/api/reminders/upcoming`).flush({
      content: [],
      page: 0,
      size: 100,
      totalElements: 0,
      totalPages: 0,
      last: true,
    });

    const result = await resultPromise;
    expect(result.stats.map((stat) => stat.value)).toEqual(['0', '0', '0', '0', '0', '0', '0', '0 days', '0']);
    expect(result.tasks).toEqual([]);
    expect(result.goals).toEqual([]);
    expect(result.habits).toEqual([]);
    expect(result.reminders).toEqual([]);
  });

  it('surfaces API failures instead of returning fabricated dashboard data', async () => {
    const resultPromise = firstValueFrom(service.loadDashboard()).then(
      () => null,
      (error: unknown) => error,
    );

    const tasksRequest = http.expectOne(
      (request) => request.url === `${API_BASE_URL}/api/tasks`,
    );
    http.expectOne(`${API_BASE_URL}/api/tasks/statistics`).flush({ pendingTasks: 0 });
    http.expectOne(`${API_BASE_URL}/api/goals`).flush([]);
    http
      .expectOne(`${API_BASE_URL}/api/goals/statistics`)
      .flush({ notStarted: 0, inProgress: 0 });
    http.expectOne(`${API_BASE_URL}/api/habits`).flush([]);
    http.expectOne((request) => request.url === `${API_BASE_URL}/api/reminders/upcoming`).flush({
      content: [],
      page: 0,
      size: 100,
      totalElements: 0,
      totalPages: 0,
      last: true,
    });
    tasksRequest.flush(
      { message: 'Task API unavailable' },
      { status: 503, statusText: 'Unavailable' },
    );

    expect(await resultPromise).toBeTruthy();
  });

  it('uses the supported task completion endpoints', () => {
    service.setTaskCompleted('11', true).subscribe();
    const completeRequest = http.expectOne(`${API_BASE_URL}/api/tasks/11/complete`);
    expect(completeRequest.request.method).toBe('PATCH');
    completeRequest.flush({
      id: 11,
      title: 'Review pull request',
      priority: 'HIGH',
      status: 'COMPLETED',
      dueDate: null,
    });

    service.setTaskCompleted('11', false).subscribe();
    const statusRequest = http.expectOne(`${API_BASE_URL}/api/tasks/11/status`);
    expect(statusRequest.request.method).toBe('PATCH');
    expect(statusRequest.request.body).toEqual({ status: 'TODO' });
    statusRequest.flush({
      id: 11,
      title: 'Review pull request',
      priority: 'HIGH',
      status: 'TODO',
      dueDate: null,
    });
  });
});

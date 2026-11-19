import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/api.config';
import { ScheduleApiService } from './schedule-api.service';

describe('ScheduleApiService', () => {
  let service: ScheduleApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ScheduleApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads all paginated schedule entries and uses daily and weekly endpoints', async () => {
    const result = service.listAll().subscribe();
    const first = http.expectOne((request) => request.url === `${API_BASE_URL}/api/schedule`);
    expect(first.request.params.get('page')).toBe('0');
    expect(first.request.params.get('size')).toBe('100');
    first.flush({
      content: [{ id: 1, title: 'Page one' }],
      page: 0,
      size: 100,
      totalElements: 101,
      totalPages: 2,
      last: false,
    });
    const second = http.expectOne(
      (request) =>
        request.url === `${API_BASE_URL}/api/schedule` && request.params.get('page') === '1',
    );
    second.flush({
      content: [{ id: 2, title: 'Page two' }],
      page: 1,
      size: 100,
      totalElements: 101,
      totalPages: 2,
      last: true,
    });
    result.unsubscribe();

    service.daily('2026-11-11').subscribe();
    const daily = http.expectOne((request) => request.url === `${API_BASE_URL}/api/schedule/daily`);
    expect(daily.request.params.get('date')).toBe('2026-11-11');
    daily.flush([]);

    service.weekly('2026-11-09').subscribe();
    const weekly = http.expectOne(
      (request) => request.url === `${API_BASE_URL}/api/schedule/weekly`,
    );
    expect(weekly.request.params.get('startDate')).toBe('2026-11-09');
    weekly.flush([]);
  });

  it('creates, updates, deletes, and checks schedule conflicts', () => {
    const body = {
      title: 'Focus block',
      description: null,
      startDateTime: '2026-11-11T09:00:00',
      endDateTime: '2026-11-11T10:00:00',
      taskId: 7,
    };

    service.create(body).subscribe();
    const create = http.expectOne(`${API_BASE_URL}/api/schedule`);
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual(body);
    create.flush({ id: 4, ...body, taskTitle: 'Write report' });

    service.update(4, body).subscribe();
    const update = http.expectOne(`${API_BASE_URL}/api/schedule/4`);
    expect(update.request.method).toBe('PUT');
    expect(update.request.body).toEqual(body);
    update.flush({ id: 4, ...body });

    service.detectConflicts(body).subscribe();
    const conflict = http.expectOne(`${API_BASE_URL}/api/schedule/conflicts`);
    expect(conflict.request.method).toBe('POST');
    expect(conflict.request.body).toEqual(body);
    conflict.flush({
      hasConflicts: true,
      conflicts: [{ type: 'TASK', id: 7, title: 'Write report' }],
    });

    service.delete(4).subscribe();
    const deletion = http.expectOne(`${API_BASE_URL}/api/schedule/4`);
    expect(deletion.request.method).toBe('DELETE');
    deletion.flush(null);
  });

  it('approves a daily plan through one shared schedule operation', () => {
    const request = {
      date: '2026-11-14',
      items: [{ taskId: 7, startTime: '09:00', endTime: '10:00' }],
    };
    service.approveDailyPlan(request).subscribe();
    const approval = http.expectOne(`${API_BASE_URL}/api/schedule/approve-daily-plan`);
    expect(approval.request.method).toBe('POST');
    expect(approval.request.body).toEqual(request);
    approval.flush([]);
  });
});

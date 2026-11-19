import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/api.config';
import { HabitApiService } from './habit-api.service';

describe('HabitApiService', () => {
  let service: HabitApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(HabitApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('uses the supported habit CRUD, deactivate, and statistics endpoints', () => {
    const body = {
      name: 'Walk',
      description: 'Take a walk',
      frequency: 'DAILY' as const,
      targetDaysPerWeek: 7,
      startDate: '2026-06-01',
      endDate: null,
    };

    service.list().subscribe();
    const list = http.expectOne(`${API_BASE_URL}/api/habits`);
    expect(list.request.method).toBe('GET');
    list.flush([]);

    service.get(4).subscribe();
    const detail = http.expectOne(`${API_BASE_URL}/api/habits/4`);
    expect(detail.request.method).toBe('GET');
    detail.flush({ id: 4, ...body, active: true });

    service.create(body).subscribe();
    const create = http.expectOne(`${API_BASE_URL}/api/habits`);
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual(body);
    create.flush({ id: 4, ...body, active: true });

    service.update(4, body).subscribe();
    const update = http.expectOne(`${API_BASE_URL}/api/habits/4`);
    expect(update.request.method).toBe('PUT');
    expect(update.request.body).toEqual(body);
    update.flush({ id: 4, ...body, active: true });

    service.deactivate(4).subscribe();
    const deactivate = http.expectOne(`${API_BASE_URL}/api/habits/4/deactivate`);
    expect(deactivate.request.method).toBe('PATCH');
    expect(deactivate.request.body).toBeNull();
    deactivate.flush({ id: 4, ...body, active: false });

    service.delete(4).subscribe();
    const deletion = http.expectOne(`${API_BASE_URL}/api/habits/4`);
    expect(deletion.request.method).toBe('DELETE');
    deletion.flush(null);

    service.statistics().subscribe();
    const stats = http.expectOne(`${API_BASE_URL}/api/habits/statistics`);
    expect(stats.request.method).toBe('GET');
    stats.flush({
      totalHabits: 1,
      activeHabits: 0,
      totalCompletedDays: 2,
      completionsThisWeek: 0,
      completionsThisMonth: 1,
      weeklyConsistencyPercentage: 0,
      monthlyConsistencyPercentage: 20,
    });
  });

  it('propagates habit API failures', () => {
    let receivedError: unknown;
    service.list().subscribe({ error: (error: unknown) => (receivedError = error) });
    http
      .expectOne(`${API_BASE_URL}/api/habits`)
      .flush({ message: 'Unavailable' }, { status: 503, statusText: 'Unavailable' });
    expect(receivedError).toBeTruthy();
  });
});

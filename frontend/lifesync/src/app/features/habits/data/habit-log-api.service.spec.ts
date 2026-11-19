import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/api.config';
import { HabitLogApiService } from './habit-log-api.service';

describe('HabitLogApiService', () => {
  let service: HabitLogApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(HabitLogApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('uses supported habit log, complete, streak, and calendar endpoints', () => {
    const logRequest = {
      completionDate: '2026-06-15',
      completed: false,
      notes: 'Rest day',
    };

    service.create(4, logRequest).subscribe();
    const create = http.expectOne(`${API_BASE_URL}/api/habits/4/logs`);
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual(logRequest);
    create.flush({ id: 8, habitId: 4, ...logRequest });

    service.list(4).subscribe();
    const list = http.expectOne(`${API_BASE_URL}/api/habits/4/logs`);
    expect(list.request.method).toBe('GET');
    list.flush([]);

    service.complete(4, { completionDate: '2026-06-15', notes: 'Done' }).subscribe();
    const complete = http.expectOne(`${API_BASE_URL}/api/habits/4/complete`);
    expect(complete.request.method).toBe('PATCH');
    expect(complete.request.body).toEqual({
      completionDate: '2026-06-15',
      notes: 'Done',
    });
    complete.flush({ id: 8, habitId: 4, completionDate: '2026-06-15', completed: true });

    service.streak(4).subscribe();
    const streak = http.expectOne(`${API_BASE_URL}/api/habits/4/streak`);
    expect(streak.request.method).toBe('GET');
    streak.flush({
      currentStreak: 3,
      longestStreak: 7,
      totalCompletedDays: 20,
      completionPercentage: 66.67,
      periodUnit: 'days',
    });

    service.calendar(4, 2026, 6).subscribe();
    const calendar = http.expectOne(
      (request) => request.url === `${API_BASE_URL}/api/habits/4/calendar`,
    );
    expect(calendar.request.method).toBe('GET');
    expect(calendar.request.params.get('year')).toBe('2026');
    expect(calendar.request.params.get('month')).toBe('6');
    calendar.flush({
      habitId: 4,
      year: 2026,
      month: 6,
      completedDays: 1,
      eligibleDays: 15,
      completionPercentage: 25,
      days: [{ date: '2026-06-15', eligible: true, completed: true, notes: 'Done' }],
    });
  });

  it('propagates log API failures', () => {
    let receivedError: unknown;
    service.list(4).subscribe({ error: (error: unknown) => (receivedError = error) });
    http
      .expectOne(`${API_BASE_URL}/api/habits/4/logs`)
      .flush({ message: 'Unavailable' }, { status: 503, statusText: 'Unavailable' });
    expect(receivedError).toBeTruthy();
  });
});

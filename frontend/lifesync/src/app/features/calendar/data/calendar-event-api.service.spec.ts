import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/api.config';
import { CalendarEventApiService } from './calendar-event-api.service';

describe('CalendarEventApiService', () => {
  let service: CalendarEventApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CalendarEventApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('uses the supported month, week, day, and date-range event queries', () => {
    service.month(2026, 11).subscribe();
    const month = http.expectOne(
      (request) => request.url === `${API_BASE_URL}/api/calendar/events/month`,
    );
    expect(month.request.params.get('year')).toBe('2026');
    expect(month.request.params.get('month')).toBe('11');
    month.flush([]);

    service.week('2026-11-09').subscribe();
    const week = http.expectOne(
      (request) => request.url === `${API_BASE_URL}/api/calendar/events/week`,
    );
    expect(week.request.params.get('startDate')).toBe('2026-11-09');
    week.flush([]);

    service.day('2026-11-11').subscribe();
    const day = http.expectOne(
      (request) => request.url === `${API_BASE_URL}/api/calendar/events/day`,
    );
    expect(day.request.params.get('date')).toBe('2026-11-11');
    day.flush([]);

    service.list('2026-11-01T00:00:00', '2026-12-01T00:00:00', 'MEETING').subscribe();
    const list = http.expectOne((request) => request.url === `${API_BASE_URL}/api/calendar/events`);
    expect(list.request.params.get('startDateTime')).toBe('2026-11-01T00:00:00');
    expect(list.request.params.get('endDateTime')).toBe('2026-12-01T00:00:00');
    expect(list.request.params.get('eventType')).toBe('MEETING');
    list.flush([]);
  });

  it('creates, loads, updates, and deletes calendar events with actual request fields', () => {
    const body = {
      title: 'Planning',
      description: 'Weekly plan',
      eventType: 'WORK' as const,
      allDay: false,
      startDateTime: '2026-11-11T09:00:00',
      endDateTime: '2026-11-11T10:00:00',
      recurrenceType: 'WEEKLY' as const,
      taskId: 7,
    };

    service.create(body).subscribe();
    const create = http.expectOne(`${API_BASE_URL}/api/calendar/events`);
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual(body);
    create.flush({ id: 8, ...body, taskTitle: 'Write report' });

    service.get(8).subscribe();
    const detail = http.expectOne(`${API_BASE_URL}/api/calendar/events/8`);
    expect(detail.request.method).toBe('GET');
    detail.flush({ id: 8, ...body });

    service.update(8, body).subscribe();
    const update = http.expectOne(`${API_BASE_URL}/api/calendar/events/8`);
    expect(update.request.method).toBe('PUT');
    expect(update.request.body).toEqual(body);
    update.flush({ id: 8, ...body });

    service.delete(8).subscribe();
    const deletion = http.expectOne(`${API_BASE_URL}/api/calendar/events/8`);
    expect(deletion.request.method).toBe('DELETE');
    deletion.flush(null);
  });

  it('propagates event API errors', () => {
    let receivedError: unknown;
    service.month(2026, 11).subscribe({ error: (error: unknown) => (receivedError = error) });
    http
      .expectOne(`${API_BASE_URL}/api/calendar/events/month?year=2026&month=11`)
      .flush({ message: 'Unavailable' }, { status: 503, statusText: 'Unavailable' });
    expect(receivedError).toBeTruthy();
  });
});

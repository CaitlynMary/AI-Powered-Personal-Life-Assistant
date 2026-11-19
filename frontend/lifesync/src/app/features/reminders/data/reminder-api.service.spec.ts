import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/api.config';
import { ReminderApiService } from './reminder-api.service';
import { ReminderRequest, ReminderResponse } from './reminder.models';

const request: ReminderRequest = {
  title: 'Call the dentist',
  message: 'Ask about the next appointment.',
  reminderDateTime: '2026-11-18T10:30:00',
  reminderType: 'GENERAL',
};

function response(id = 1): ReminderResponse {
  return {
    ...request,
    id,
    status: 'PENDING',
    createdAt: '2026-11-12T09:00:00',
    updatedAt: '2026-11-12T09:00:00',
  };
}

describe('ReminderApiService', () => {
  let service: ReminderApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ReminderApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('creates, loads, updates, and cancels reminders using the supported API', () => {
    service.create(request).subscribe();
    const create = http.expectOne(`${API_BASE_URL}/api/reminders`);
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual(request);
    create.flush(response());

    service.get(1).subscribe();
    const get = http.expectOne(`${API_BASE_URL}/api/reminders/1`);
    expect(get.request.method).toBe('GET');
    get.flush(response());

    service.update(1, request).subscribe();
    const update = http.expectOne(`${API_BASE_URL}/api/reminders/1`);
    expect(update.request.method).toBe('PUT');
    expect(update.request.body).toEqual(request);
    update.flush(response());

    service.cancel(1).subscribe();
    const cancel = http.expectOne(`${API_BASE_URL}/api/reminders/1`);
    expect(cancel.request.method).toBe('DELETE');
    cancel.flush(null);
  });

  it('loads all pages and supports the upcoming reminder query', () => {
    service
      .listAll()
      .subscribe((reminders) => expect(reminders.map((item) => item.id)).toEqual([1, 2]));
    const first = http.expectOne(
      (request) =>
        request.url === `${API_BASE_URL}/api/reminders` &&
        request.params.get('page') === '0' &&
        request.params.get('size') === '100',
    );
    first.flush({
      content: [response(1)],
      page: 0,
      size: 100,
      totalElements: 2,
      totalPages: 2,
      last: false,
    });
    const second = http.expectOne(
      (request) =>
        request.url === `${API_BASE_URL}/api/reminders` && request.params.get('page') === '1',
    );
    second.flush({
      content: [response(2)],
      page: 1,
      size: 100,
      totalElements: 2,
      totalPages: 2,
      last: true,
    });

    service.upcoming(0, 10).subscribe();
    const upcoming = http.expectOne(
      (request) => request.url === `${API_BASE_URL}/api/reminders/upcoming`,
    );
    expect(upcoming.request.params.get('size')).toBe('10');
    upcoming.flush({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
      last: true,
    });
  });
});

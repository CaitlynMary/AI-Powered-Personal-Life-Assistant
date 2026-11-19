import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/api.config';
import { GoalApiService } from './goal-api.service';

describe('GoalApiService', () => {
  let service: GoalApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(GoalApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('uses the supported goal list, detail, create, update, status, and delete endpoints', () => {
    const body = {
      title: 'Learn Angular',
      description: 'Build a project',
      goalType: 'MONTHLY' as const,
      startDate: '2026-06-01',
      targetDate: '2026-06-30',
    };

    service.list().subscribe();
    const list = http.expectOne(`${API_BASE_URL}/api/goals`);
    expect(list.request.method).toBe('GET');
    list.flush([]);

    service.get(12).subscribe();
    const detail = http.expectOne(`${API_BASE_URL}/api/goals/12`);
    expect(detail.request.method).toBe('GET');
    detail.flush({ id: 12, ...body, status: 'NOT_STARTED', progressPercentage: 0 });

    service.create(body).subscribe();
    const create = http.expectOne(`${API_BASE_URL}/api/goals`);
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual(body);
    create.flush({ id: 12, ...body, status: 'NOT_STARTED', progressPercentage: 0 });

    service.update(12, body).subscribe();
    const update = http.expectOne(`${API_BASE_URL}/api/goals/12`);
    expect(update.request.method).toBe('PUT');
    expect(update.request.body).toEqual(body);
    update.flush({ id: 12, ...body, status: 'IN_PROGRESS', progressPercentage: 50 });

    service.updateStatus(12, 'COMPLETED').subscribe();
    const status = http.expectOne(`${API_BASE_URL}/api/goals/12/status`);
    expect(status.request.method).toBe('PATCH');
    expect(status.request.body).toEqual({ status: 'COMPLETED' });
    status.flush({ id: 12, ...body, status: 'COMPLETED', progressPercentage: 100 });

    service.delete(12).subscribe();
    const deletion = http.expectOne(`${API_BASE_URL}/api/goals/12`);
    expect(deletion.request.method).toBe('DELETE');
    deletion.flush(null);
  });

  it('propagates goal API errors', () => {
    let receivedError: unknown;
    service.list().subscribe({ error: (error: unknown) => (receivedError = error) });
    http
      .expectOne(`${API_BASE_URL}/api/goals`)
      .flush({ message: 'Unavailable' }, { status: 503, statusText: 'Unavailable' });
    expect(receivedError).toBeTruthy();
  });
});

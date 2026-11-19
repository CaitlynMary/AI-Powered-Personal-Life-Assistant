import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/api.config';
import { MilestoneApiService } from './milestone-api.service';

describe('MilestoneApiService', () => {
  let service: MilestoneApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(MilestoneApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('uses supported milestone list, detail, create, update, complete, and delete endpoints', () => {
    const body = {
      title: 'Finish first lesson',
      description: null,
      targetDate: '2026-06-15',
    };

    service.listForGoal(12).subscribe();
    const list = http.expectOne(`${API_BASE_URL}/api/goals/12/milestones`);
    expect(list.request.method).toBe('GET');
    list.flush([]);

    service.get(21).subscribe();
    const detail = http.expectOne(`${API_BASE_URL}/api/milestones/21`);
    expect(detail.request.method).toBe('GET');
    detail.flush({ id: 21, goalId: 12, ...body, status: 'NOT_STARTED' });

    service.create(12, body).subscribe();
    const create = http.expectOne(`${API_BASE_URL}/api/goals/12/milestones`);
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual(body);
    create.flush({ id: 21, goalId: 12, ...body, status: 'NOT_STARTED' });

    service.update(21, body).subscribe();
    const update = http.expectOne(`${API_BASE_URL}/api/milestones/21`);
    expect(update.request.method).toBe('PUT');
    expect(update.request.body).toEqual(body);
    update.flush({ id: 21, goalId: 12, ...body, status: 'NOT_STARTED' });

    service.complete(21).subscribe();
    const complete = http.expectOne(`${API_BASE_URL}/api/milestones/21/complete`);
    expect(complete.request.method).toBe('PATCH');
    expect(complete.request.body).toBeNull();
    complete.flush({ id: 21, goalId: 12, ...body, status: 'COMPLETED' });

    service.delete(21).subscribe();
    const deletion = http.expectOne(`${API_BASE_URL}/api/milestones/21`);
    expect(deletion.request.method).toBe('DELETE');
    deletion.flush(null);
  });

  it('propagates milestone API errors', () => {
    let receivedError: unknown;
    service.listForGoal(12).subscribe({ error: (error: unknown) => (receivedError = error) });
    http
      .expectOne(`${API_BASE_URL}/api/goals/12/milestones`)
      .flush({ message: 'Unavailable' }, { status: 503, statusText: 'Unavailable' });
    expect(receivedError).toBeTruthy();
  });
});

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/api.config';
import { TaskApiService } from './task-api.service';
import { TaskRequest } from './task.models';

describe('TaskApiService', () => {
  let service: TaskApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TaskApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists tasks with supported filters and pagination', () => {
    service
      .list({
        page: 2,
        size: 20,
        status: 'TODO',
        priority: 'HIGH',
        categoryId: 7,
        search: '  write docs ',
        sortBy: 'dueDate',
        direction: 'ASC',
      })
      .subscribe();

    const request = http.expectOne((req) => req.url === `${API_BASE_URL}/api/tasks`);
    expect(request.request.method).toBe('GET');
    expect(request.request.params.keys()).toEqual([
      'page',
      'size',
      'sortBy',
      'direction',
      'status',
      'priority',
      'categoryId',
      'search',
    ]);
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('20');
    expect(request.request.params.get('status')).toBe('TODO');
    expect(request.request.params.get('priority')).toBe('HIGH');
    expect(request.request.params.get('categoryId')).toBe('7');
    expect(request.request.params.get('search')).toBe('write docs');
    request.flush({ content: [], page: 2, size: 20, totalElements: 0, totalPages: 0, last: true });
  });

  it('fetches all pages with the calendar due-date range filters', () => {
    const result: unknown[] = [];
    service
      .listAll({
        dueFrom: '2026-11-01T00:00:00',
        dueTo: '2026-11-30T23:59:59.999999999',
      })
      .subscribe((tasks) => result.push(...tasks));

    const first = http.expectOne((request) => request.url === `${API_BASE_URL}/api/tasks`);
    expect(first.request.params.get('page')).toBe('0');
    expect(first.request.params.get('size')).toBe('100');
    expect(first.request.params.get('dueFrom')).toBe('2026-11-01T00:00:00');
    expect(first.request.params.get('dueTo')).toBe('2026-11-30T23:59:59.999999999');
    first.flush({
      content: [{ id: 1 }],
      page: 0,
      size: 100,
      totalElements: 101,
      totalPages: 2,
      last: false,
    });

    const second = http.expectOne(
      (request) =>
        request.url === `${API_BASE_URL}/api/tasks` && request.params.get('page') === '1',
    );
    expect(second.request.params.get('dueFrom')).toBe('2026-11-01T00:00:00');
    expect(second.request.params.get('dueTo')).toBe('2026-11-30T23:59:59.999999999');
    second.flush({
      content: [{ id: 2 }],
      page: 1,
      size: 100,
      totalElements: 101,
      totalPages: 2,
      last: true,
    });
    expect(result).toEqual([{ id: 1 }, { id: 2 }]);
  });

  it('creates and updates tasks using backend request fields', () => {
    const body: TaskRequest = {
      title: 'Write docs',
      description: null,
      categoryId: 7,
      goalId: null,
      milestoneId: null,
      priority: 'HIGH',
      dueDate: null,
      startDate: null,
      estimatedDuration: null,
      recurring: false,
      recurrenceType: 'NONE',
    };

    service.create(body).subscribe();
    const createRequest = http.expectOne(`${API_BASE_URL}/api/tasks`);
    expect(createRequest.request.method).toBe('POST');
    expect(createRequest.request.body).toEqual(body);
    createRequest.flush({ id: 1, ...body, status: 'TODO' });

    service.update(1, body).subscribe();
    const updateRequest = http.expectOne(`${API_BASE_URL}/api/tasks/1`);
    expect(updateRequest.request.method).toBe('PUT');
    expect(updateRequest.request.body).toEqual(body);
    updateRequest.flush({ id: 1, ...body, status: 'TODO' });
  });

  it('completes, changes status, and deletes tasks through supported endpoints', () => {
    service.complete(3).subscribe();
    const completeRequest = http.expectOne(`${API_BASE_URL}/api/tasks/3/complete`);
    expect(completeRequest.request.method).toBe('PATCH');
    completeRequest.flush({ id: 3, status: 'COMPLETED' });

    service.setStatus(3, 'TODO').subscribe();
    const statusRequest = http.expectOne(`${API_BASE_URL}/api/tasks/3/status`);
    expect(statusRequest.request.method).toBe('PATCH');
    expect(statusRequest.request.body).toEqual({ status: 'TODO' });
    statusRequest.flush({ id: 3, status: 'TODO' });

    service.delete(3).subscribe();
    const deleteRequest = http.expectOne(`${API_BASE_URL}/api/tasks/3`);
    expect(deleteRequest.request.method).toBe('DELETE');
    deleteRequest.flush(null);
  });

  it('propagates task API failures to the caller', () => {
    let receivedError: unknown;
    service.list().subscribe({ error: (error: unknown) => (receivedError = error) });
    http
      .expectOne((request) => request.url === `${API_BASE_URL}/api/tasks`)
      .flush({ message: 'Unavailable' }, { status: 503, statusText: 'Unavailable' });

    expect(receivedError).toBeTruthy();
  });
});

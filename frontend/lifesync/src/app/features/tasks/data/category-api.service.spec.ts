import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/api.config';
import { CategoryApiService } from './category-api.service';

describe('CategoryApiService', () => {
  let service: CategoryApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CategoryApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads categories from the authenticated categories endpoint', () => {
    service.list().subscribe();
    const request = http.expectOne(`${API_BASE_URL}/api/categories`);
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('creates, updates, and deletes categories', () => {
    const body = { name: 'Work', description: 'Work items', color: '#81689D' };

    service.create(body).subscribe();
    const createRequest = http.expectOne(`${API_BASE_URL}/api/categories`);
    expect(createRequest.request.method).toBe('POST');
    expect(createRequest.request.body).toEqual(body);
    createRequest.flush({ id: 4, ...body });

    service.update(4, body).subscribe();
    const updateRequest = http.expectOne(`${API_BASE_URL}/api/categories/4`);
    expect(updateRequest.request.method).toBe('PUT');
    expect(updateRequest.request.body).toEqual(body);
    updateRequest.flush({ id: 4, ...body });

    service.delete(4).subscribe();
    const deleteRequest = http.expectOne(`${API_BASE_URL}/api/categories/4`);
    expect(deleteRequest.request.method).toBe('DELETE');
    deleteRequest.flush(null);
  });

  it('propagates category API failures to the caller', () => {
    let receivedError: unknown;
    service.list().subscribe({ error: (error: unknown) => (receivedError = error) });
    http.expectOne(`${API_BASE_URL}/api/categories`).flush(
      { message: 'Unavailable' },
      { status: 503, statusText: 'Unavailable' },
    );

    expect(receivedError).toBeTruthy();
  });
});

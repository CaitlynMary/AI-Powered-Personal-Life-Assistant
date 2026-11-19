import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/api.config';
import { NotificationApiService } from './notification-api.service';

describe('NotificationApiService', () => {
  let service: NotificationApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(NotificationApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('uses the supported list, unread-count, and read operations', () => {
    service.list(2, 20, true).subscribe();
    const list = http.expectOne((request) => request.url === `${API_BASE_URL}/api/notifications`);
    expect(list.request.params.get('page')).toBe('2');
    expect(list.request.params.get('size')).toBe('20');
    expect(list.request.params.get('unreadOnly')).toBe('true');
    list.flush({ content: [], page: 2, size: 20, totalElements: 0, totalPages: 0, last: true });

    service.unreadCount().subscribe();
    http.expectOne(`${API_BASE_URL}/api/notifications/unread-count`).flush({ unreadCount: 3 });

    service.markRead(5).subscribe();
    const markRead = http.expectOne(`${API_BASE_URL}/api/notifications/5/read`);
    expect(markRead.request.method).toBe('PATCH');
    markRead.flush(null);

    service.markAllRead().subscribe();
    const markAll = http.expectOne(`${API_BASE_URL}/api/notifications/read-all`);
    expect(markAll.request.method).toBe('PATCH');
    markAll.flush(null);
  });

  it('loads all pages without dropping notification records', () => {
    let result: number[] = [];
    service.listAll().subscribe((items) => (result = items.map((item) => item.id)));
    http
      .expectOne(
        (request) =>
          request.url === `${API_BASE_URL}/api/notifications` &&
          request.params.get('size') === '100',
      )
      .flush({
        content: [
          {
            id: 7,
            title: 'First',
            message: '',
            notificationType: 'SYSTEM',
            isRead: false,
            reminderId: null,
            createdAt: '2026-11-12T09:00:00',
          },
        ],
        page: 0,
        size: 100,
        totalElements: 2,
        totalPages: 2,
        last: false,
      });
    http
      .expectOne(
        (request) =>
          request.url === `${API_BASE_URL}/api/notifications` && request.params.get('page') === '1',
      )
      .flush({
        content: [
          {
            id: 8,
            title: 'Second',
            message: '',
            notificationType: 'REMINDER',
            isRead: true,
            reminderId: 3,
            createdAt: '2026-11-11T09:00:00',
          },
        ],
        page: 1,
        size: 100,
        totalElements: 2,
        totalPages: 2,
        last: true,
      });
    expect(result).toEqual([7, 8]);
  });
});

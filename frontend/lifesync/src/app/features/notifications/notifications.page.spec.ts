import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { API_BASE_URL } from '../../core/api.config';
import { NotificationsPage } from './notifications.page';
import { NotificationResponse } from './data/notification.models';

const notifications: NotificationResponse[] = [
  {
    id: 1,
    title: 'Reminder is due',
    message: 'Take a stretch break.',
    notificationType: 'REMINDER' as const,
    isRead: false,
    reminderId: 8,
    createdAt: '2026-11-12T09:00:00',
  },
  {
    id: 2,
    title: 'A helpful update',
    message: 'Your profile is ready.',
    notificationType: 'SYSTEM' as const,
    isRead: true,
    reminderId: null,
    createdAt: '2026-11-11T09:00:00',
  },
];

function pageResponse(content: NotificationResponse[] = notifications) {
  return {
    content,
    page: 0,
    size: 100,
    totalElements: content.length,
    totalPages: content.length ? 1 : 0,
    last: true,
  };
}

describe('NotificationsPage', () => {
  let fixture: ComponentFixture<NotificationsPage>;
  let component: NotificationsPage;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    fixture = TestBed.createComponent(NotificationsPage);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify({ ignoreCancelled: true }));

  function flushList(items: NotificationResponse[] = notifications): void {
    fixture.detectChanges();
    http
      .expectOne(
        (request) =>
          request.url === `${API_BASE_URL}/api/notifications` &&
          request.params.get('size') === '100',
      )
      .flush(pageResponse(items));
    http
      .expectOne(`${API_BASE_URL}/api/notifications/unread-count`)
      .flush({ unreadCount: items.filter((item) => !item.isRead).length });
    fixture.detectChanges();
  }

  it('renders notifications and filters read status and search text', () => {
    flushList();
    expect(fixture.nativeElement.textContent).toContain('Notifications');
    expect(fixture.nativeElement.textContent).toContain('Reminder is due');
    expect(component.unreadCount()).toBe(1);

    component.setReadFilter('UNREAD');
    expect(component.visibleNotifications().map((item) => item.id)).toEqual([1]);
    component.setReadFilter('READ');
    expect(component.visibleNotifications().map((item) => item.id)).toEqual([2]);
    component.setReadFilter('ALL');
    component.searchText.set('  PROFILE  ');
    expect(component.visibleNotifications().map((item) => item.id)).toEqual([2]);
    component.searchText.set('FILE IS');
    expect(component.visibleNotifications().map((item) => item.id)).toEqual([2]);
    component.searchText.set('no-such-notification');
    expect(component.visibleNotifications()).toEqual([]);
    component.searchText.set('');
    expect(component.visibleNotifications()).toHaveLength(2);
  });

  it('filters categories and displays their related entities', () => {
    const items = [
      {
        ...notifications[0],
        notificationType: 'MILESTONE_COMPLETED' as const,
        milestoneId: 7,
        milestoneTitle: 'Finish capstone',
        goalId: 4,
        goalTitle: 'Graduate',
      },
      {
        ...notifications[1],
        id: 3,
        notificationType: 'HABIT_COMPLETED' as const,
        habitId: 12,
        habitName: 'Read',
      },
    ];
    flushList(items);

    component.setCategoryFilter('MILESTONE_COMPLETED');

    expect(component.visibleNotifications().map((item) => item.id)).toEqual([1]);
    expect(component.typeLabel('MILESTONE_COMPLETED')).toBe('Milestone completed');
    expect(fixture.nativeElement.textContent).toContain('Finish capstone');
    expect(fixture.nativeElement.textContent).toContain('Graduate');
    expect(fixture.nativeElement.textContent).toContain('Read');
  });

  it('marks a notification and all notifications as read only after backend success', () => {
    flushList();
    component.markRead(notifications[0]);
    const markRead = http.expectOne(`${API_BASE_URL}/api/notifications/1/read`);
    expect(markRead.request.method).toBe('PATCH');
    markRead.flush(null);
    expect(component.unreadCount()).toBe(0);

    component.notifications.set([{ ...notifications[0], isRead: false }, notifications[1]]);
    component.markAllRead();
    const markAll = http.expectOne(`${API_BASE_URL}/api/notifications/read-all`);
    expect(markAll.request.method).toBe('PATCH');
    markAll.flush(null);
    expect(component.notifications().every((item) => item.isRead)).toBe(true);
    expect(component.success()).toBe('All notifications marked as read.');
  });

  it('shows empty, loading, and retryable error states', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Gathering your updates');
    http
      .expectOne((request) => request.url === `${API_BASE_URL}/api/notifications`)
      .flush(pageResponse([]));
    http.expectOne(`${API_BASE_URL}/api/notifications/unread-count`).flush({ unreadCount: 0 });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('You’re all caught up');

    component.loadNotifications();
    http
      .expectOne((request) => request.url === `${API_BASE_URL}/api/notifications`)
      .flush(
        { message: 'Notification service unavailable' },
        { status: 503, statusText: 'Unavailable' },
      );
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Notification service unavailable');
    expect(fixture.nativeElement.textContent).toContain('Retry');
  });
});

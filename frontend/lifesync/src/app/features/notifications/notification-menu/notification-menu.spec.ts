import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { LucideBell, provideLucideIcons } from '@lucide/angular';
import { API_BASE_URL } from '../../../core/api.config';
import { NotificationMenu } from './notification-menu';

describe('NotificationMenu', () => {
  let fixture: ComponentFixture<NotificationMenu>;
  let component: NotificationMenu;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideLucideIcons(LucideBell),
      ],
    });
    fixture = TestBed.createComponent(NotificationMenu);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify({ ignoreCancelled: true }));

  it('loads the unread badge and recent items when the panel opens', () => {
    fixture.detectChanges();
    http.expectOne(`${API_BASE_URL}/api/notifications/unread-count`).flush({ unreadCount: 1 });
    component.toggle();
    const list = http.expectOne(
      (request) =>
        request.url === `${API_BASE_URL}/api/notifications` && request.params.get('size') === '5',
    );
    list.flush({
      content: [
        {
          id: 3,
          title: 'A gentle reminder',
          message: 'Take a short walk.',
          notificationType: 'REMINDER',
          isRead: false,
          reminderId: 2,
          createdAt: '2026-11-12T09:00:00',
        },
      ],
      page: 0,
      size: 5,
      totalElements: 1,
      totalPages: 1,
      last: true,
    });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('A gentle reminder');
    expect(fixture.nativeElement.textContent).toContain('View all');
    expect(component.state.unreadCount()).toBe(1);
  });
});

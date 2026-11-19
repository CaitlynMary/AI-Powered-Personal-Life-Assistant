import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, expand, of, reduce } from 'rxjs';
import { API_BASE_URL } from '../../../core/api.config';
import {
  NotificationPageResponse,
  NotificationResponse,
  UnreadCountResponse,
} from './notification.models';

const PAGE_SIZE = 100;

@Injectable({ providedIn: 'root' })
export class NotificationApiService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE_URL}/api/notifications`;

  list(page = 0, size = 20, unreadOnly = false): Observable<NotificationPageResponse> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('unreadOnly', unreadOnly);
    return this.http.get<NotificationPageResponse>(this.url, { params });
  }

  listAll(): Observable<NotificationResponse[]> {
    return this.list(0, PAGE_SIZE).pipe(
      expand((page) => (page.last ? of() : this.list(page.page + 1, PAGE_SIZE))),
      reduce(
        (notifications, page) => [...notifications, ...page.content],
        [] as NotificationResponse[],
      ),
    );
  }

  unreadCount(): Observable<UnreadCountResponse> {
    return this.http.get<UnreadCountResponse>(`${this.url}/unread-count`);
  }

  markRead(id: number): Observable<void> {
    return this.http.patch<void>(`${this.url}/${id}/read`, null);
  }

  markAllRead(): Observable<void> {
    return this.http.patch<void>(`${this.url}/read-all`, null);
  }
}

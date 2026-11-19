import { Injectable, inject, signal } from '@angular/core';
import { Observable, Subscription, finalize, tap } from 'rxjs';
import { readApiError } from '../../../core/auth/auth-error';
import { NotificationApiService } from './notification-api.service';
import { NotificationResponse } from './notification.models';

const RECENT_CACHE_MS = 60_000;

@Injectable({ providedIn: 'root' })
export class NotificationStateService {
  private readonly api = inject(NotificationApiService);
  private countRequestId = 0;
  private recentRequestId = 0;
  private countRequest?: Subscription;
  private recentRequest?: Subscription;
  private countLoaded = false;
  private recentLoadedAt = 0;

  readonly unreadCount = signal<number | null>(null);
  readonly countLoading = signal(false);
  readonly countError = signal<string | null>(null);
  readonly recent = signal<NotificationResponse[]>([]);
  readonly recentLoading = signal(false);
  readonly recentError = signal<string | null>(null);

  loadUnreadCount(force = false): void {
    if (this.countLoading() && !force) return;
    if (this.countLoaded && !force) return;
    this.countRequest?.unsubscribe();
    const requestId = ++this.countRequestId;
    this.countLoading.set(true);
    this.countError.set(null);
    this.countRequest = this.api
      .unreadCount()
      .pipe(
        finalize(() => {
          if (requestId === this.countRequestId) this.countLoading.set(false);
        }),
      )
      .subscribe({
        next: (response) => {
          if (requestId !== this.countRequestId) return;
          this.unreadCount.set(response.unreadCount);
          this.countLoaded = true;
        },
        error: (error: unknown) => {
          if (requestId !== this.countRequestId) return;
          this.countError.set(readApiError(error, 'Unread notification count is unavailable.'));
        },
      });
  }

  refreshUnreadCount(): void {
    this.loadUnreadCount(true);
  }

  loadRecent(force = false): void {
    if (this.recentLoading() && !force) return;
    if (!force && this.recentLoadedAt > 0 && Date.now() - this.recentLoadedAt < RECENT_CACHE_MS) {
      return;
    }
    this.recentRequest?.unsubscribe();
    const requestId = ++this.recentRequestId;
    this.recentLoading.set(true);
    this.recentError.set(null);
    this.recentRequest = this.api
      .list(0, 5)
      .pipe(
        finalize(() => {
          if (requestId === this.recentRequestId) this.recentLoading.set(false);
        }),
      )
      .subscribe({
        next: (response) => {
          if (requestId !== this.recentRequestId) return;
          this.recent.set(response.content);
          this.recentLoadedAt = Date.now();
        },
        error: (error: unknown) => {
          if (requestId !== this.recentRequestId) return;
          this.recentError.set(readApiError(error, 'Could not load recent notifications.'));
        },
      });
  }

  refreshRecent(): void {
    this.loadRecent(true);
  }

  markRead(notification: NotificationResponse): Observable<void> {
    return this.api.markRead(notification.id).pipe(
      tap(() => {
        this.recent.update((items) =>
          items.map((item) => (item.id === notification.id ? { ...item, isRead: true } : item)),
        );
        if (!notification.isRead && this.unreadCount() !== null) {
          this.unreadCount.update((count) => Math.max(0, (count ?? 0) - 1));
        }
      }),
    );
  }

  markAllRead(): Observable<void> {
    return this.api.markAllRead().pipe(
      tap(() => {
        this.unreadCount.set(0);
        this.countLoaded = true;
        this.recent.update((items) => items.map((item) => ({ ...item, isRead: true })));
      }),
    );
  }
}

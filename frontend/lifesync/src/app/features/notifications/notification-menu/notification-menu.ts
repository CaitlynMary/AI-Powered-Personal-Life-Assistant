import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { LucideDynamicIcon } from '@lucide/angular';
import { interval } from 'rxjs';
import { RegionalDateService } from '../../../core/services/regional-date.service';
import { readApiError } from '../../../core/auth/auth-error';
import { NotificationResponse } from '../data/notification.models';
import { NotificationStateService } from '../data/notification-state.service';

@Component({
  selector: 'app-notification-menu',
  standalone: true,
  imports: [LucideDynamicIcon, RouterLink],
  templateUrl: './notification-menu.html',
})
export class NotificationMenu {
  private readonly destroyRef = inject(DestroyRef);
  private readonly regionalDate = inject(RegionalDateService);
  readonly state = inject(NotificationStateService);
  readonly open = signal(false);
  readonly busyId = signal<number | null>(null);
  readonly busyAll = signal(false);
  readonly actionError = signal<string | null>(null);

  constructor() {
    this.state.loadUnreadCount();
    interval(60_000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.state.refreshUnreadCount());
  }

  toggle(): void {
    const next = !this.open();
    this.open.set(next);
    if (next) this.state.loadRecent();
  }

  close(): void {
    this.open.set(false);
  }

  markRead(notification: NotificationResponse): void {
    if (notification.isRead || this.busyId() !== null) return;
    this.busyId.set(notification.id);
    this.actionError.set(null);
    this.state
      .markRead(notification)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        error: (error: unknown) => {
          this.actionError.set(readApiError(error, 'Could not mark this notification as read.'));
          this.busyId.set(null);
        },
        complete: () => this.busyId.set(null),
      });
  }

  markAllRead(): void {
    if (this.busyAll() || (this.state.unreadCount() ?? 0) === 0) return;
    this.busyAll.set(true);
    this.actionError.set(null);
    this.state
      .markAllRead()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        error: (error: unknown) => {
          this.actionError.set(readApiError(error, 'Could not mark notifications as read.'));
          this.busyAll.set(false);
        },
        complete: () => this.busyAll.set(false),
      });
  }

  formatDate(value: string): string {
    return this.regionalDate.formatDateTime(value, {
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
    });
  }
}

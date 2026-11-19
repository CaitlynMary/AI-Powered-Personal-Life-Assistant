import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { readApiError } from '../../core/auth/auth-error';
import { RegionalDateService } from '../../core/services/regional-date.service';
import { NotificationApiService } from './data/notification-api.service';
import { NotificationResponse, NotificationType } from './data/notification.models';
import { NotificationStateService } from './data/notification-state.service';

const NOTIFICATIONS_PER_PAGE = 10;

@Component({
  selector: 'app-notifications-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './notifications.page.html',
})
export class NotificationsPage {
  private readonly api = inject(NotificationApiService);
  private readonly regionalDate = inject(RegionalDateService);
  readonly state = inject(NotificationStateService);
  private requestId = 0;

  readonly notifications = signal<NotificationResponse[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly searchText = signal('');
  readonly readFilter = signal<'ALL' | 'UNREAD' | 'READ'>('ALL');
  readonly categoryFilter = signal<NotificationType | 'ALL'>('ALL');
  readonly fromDate = signal('');
  readonly toDate = signal('');
  readonly page = signal(0);
  readonly busyId = signal<number | null>(null);
  readonly busyAll = signal(false);

  readonly unreadCount = computed(
    () => this.notifications().filter((notification) => !notification.isRead).length,
  );
  readonly filteredNotifications = computed(() => {
    const search = this.searchText().trim().toLowerCase();
    const filter = this.readFilter();
    const category = this.categoryFilter();
    const from = this.fromDate();
    const to = this.toDate();
    return this.notifications()
      .filter((notification) => {
        const date = notification.createdAt.slice(0, 10);
        return (
          (filter === 'ALL' ||
            (filter === 'UNREAD' && !notification.isRead) ||
            (filter === 'READ' && notification.isRead)) &&
          (category === 'ALL' || notification.notificationType === category) &&
          (!from || date >= from) &&
          (!to || date <= to) &&
          (!search ||
            notification.title.toLowerCase().includes(search) ||
            notification.message.toLowerCase().includes(search))
        );
      })
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  });
  readonly pageCount = computed(() =>
    Math.max(1, Math.ceil(this.filteredNotifications().length / NOTIFICATIONS_PER_PAGE)),
  );
  readonly visibleNotifications = computed(() =>
    this.filteredNotifications().slice(
      this.page() * NOTIFICATIONS_PER_PAGE,
      (this.page() + 1) * NOTIFICATIONS_PER_PAGE,
    ),
  );

  constructor() {
    this.loadNotifications();
  }

  loadNotifications(): void {
    const requestId = ++this.requestId;
    this.loading.set(true);
    this.error.set(null);
    this.api
      .listAll()
      .pipe(finalize(() => requestId === this.requestId && this.loading.set(false)))
      .subscribe({
        next: (notifications) => {
          if (requestId !== this.requestId) return;
          this.notifications.set(notifications);
          if (
            this.page() >= Math.max(1, Math.ceil(notifications.length / NOTIFICATIONS_PER_PAGE))
          ) {
            this.page.set(0);
          }
          this.state.refreshUnreadCount();
        },
        error: (error: unknown) => {
          if (requestId === this.requestId) {
            this.error.set(readApiError(error, 'Could not load notifications. Please try again.'));
          }
        },
      });
  }

  markRead(notification: NotificationResponse): void {
    if (notification.isRead || this.busyId() !== null) return;
    this.busyId.set(notification.id);
    this.actionError.set(null);
    this.state
      .markRead(notification)
      .pipe(finalize(() => this.busyId.set(null)))
      .subscribe({
        next: () => {
          this.notifications.update((items) =>
            items.map((item) => (item.id === notification.id ? { ...item, isRead: true } : item)),
          );
          this.success.set('Notification marked as read.');
        },
        error: (error: unknown) => {
          this.actionError.set(readApiError(error, 'Could not mark this notification as read.'));
        },
      });
  }

  markAllRead(): void {
    if (this.busyAll() || this.unreadCount() === 0) return;
    this.busyAll.set(true);
    this.actionError.set(null);
    this.state
      .markAllRead()
      .pipe(finalize(() => this.busyAll.set(false)))
      .subscribe({
        next: () => {
          this.notifications.update((items) => items.map((item) => ({ ...item, isRead: true })));
          this.success.set('All notifications marked as read.');
        },
        error: (error: unknown) => {
          this.actionError.set(readApiError(error, 'Could not mark notifications as read.'));
        },
      });
  }

  setReadFilter(value: string): void {
    if (value === 'ALL' || value === 'UNREAD' || value === 'READ') {
      this.readFilter.set(value);
      this.page.set(0);
    }
  }

  setCategoryFilter(value: string): void {
    const validTypes: NotificationType[] = [
      'REMINDER',
      'TASK_DEADLINE_APPROACHING',
      'TASK_OVERDUE',
      'TASK_COMPLETED',
      'TASK_MISSED',
      'TASK_RESCHEDULED',
      'MILESTONE_COMPLETED',
      'HABIT_COMPLETED',
      'AI_DAILY_PLAN_READY',
      'SYSTEM',
    ];
    if (value === 'ALL' || validTypes.includes(value as NotificationType)) {
      this.categoryFilter.set(value as NotificationType | 'ALL');
      this.page.set(0);
    }
  }

  typeLabel(type: NotificationType): string {
    return {
      REMINDER: 'Reminder',
      TASK_DEADLINE_APPROACHING: 'Deadline approaching',
      TASK_OVERDUE: 'Task overdue',
      TASK_COMPLETED: 'Task completed',
      TASK_MISSED: 'Task missed',
      TASK_RESCHEDULED: 'Task rescheduled',
      MILESTONE_COMPLETED: 'Milestone completed',
      HABIT_COMPLETED: 'Habit completed',
      AI_DAILY_PLAN_READY: 'AI plan ready',
      SYSTEM: 'System',
    }[type];
  }

  setDateFilter(which: 'from' | 'to', value: string): void {
    if (which === 'from') this.fromDate.set(value);
    else this.toDate.set(value);
    this.page.set(0);
  }

  changePage(offset: number): void {
    const next = this.page() + offset;
    if (next < 0 || next >= this.pageCount()) return;
    this.page.set(next);
  }

  formatDate(value: string): string {
    return this.regionalDate.formatDateTime(value, {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
    });
  }

  clearMessages(): void {
    this.actionError.set(null);
    this.success.set(null);
  }
}

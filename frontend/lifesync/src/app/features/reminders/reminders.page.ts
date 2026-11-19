import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { readApiError } from '../../core/auth/auth-error';
import { RegionalDateService } from '../../core/services/regional-date.service';
import { ConfirmationDialogService } from '../../shared/confirmation-dialog/confirmation-dialog.service';
import { ReminderApiService } from './data/reminder-api.service';
import {
  ReminderRequest,
  ReminderResponse,
  ReminderStatus,
  ReminderSummary,
  ReminderType,
} from './data/reminder.models';
import { ReminderForm } from './reminder-form/reminder-form';

const REMINDERS_PER_PAGE = 10;

@Component({
  selector: 'app-reminders-page',
  standalone: true,
  imports: [CommonModule, FormsModule, ReminderForm],
  templateUrl: './reminders.page.html',
})
export class RemindersPage {
  private readonly remindersApi = inject(ReminderApiService);
  private readonly confirmation = inject(ConfirmationDialogService);
  private readonly regionalDate = inject(RegionalDateService);
  private requestId = 0;

  readonly reminders = signal<ReminderResponse[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly searchText = signal('');
  readonly statusFilter = signal<ReminderStatus | ''>('');
  readonly fromDate = signal('');
  readonly toDate = signal('');
  readonly sortOrder = signal<'EARLIEST' | 'LATEST' | 'TITLE'>('EARLIEST');
  readonly page = signal(0);
  readonly editorOpen = signal(false);
  readonly editingReminder = signal<ReminderResponse | null>(null);
  readonly editorError = signal<string | null>(null);
  readonly saving = signal(false);
  readonly cancellingId = signal<number | null>(null);

  readonly summary = computed<ReminderSummary>(() => {
    const now = this.localDateTime(new Date());
    const reminders = this.reminders();
    return {
      total: reminders.length,
      pending: reminders.filter((reminder) => reminder.status === 'PENDING').length,
      upcoming: reminders.filter(
        (reminder) => reminder.status === 'PENDING' && reminder.reminderDateTime >= now,
      ).length,
      sent: reminders.filter((reminder) => reminder.status === 'SENT').length,
    };
  });

  readonly filteredReminders = computed(() => {
    const search = this.searchText().trim().toLowerCase();
    const status = this.statusFilter();
    const from = this.fromDate();
    const to = this.toDate();
    const filtered = this.reminders().filter((reminder) => {
      const date = reminder.reminderDateTime.slice(0, 10);
      return (
        (!status || reminder.status === status) &&
        (!from || date >= from) &&
        (!to || date <= to) &&
        (!search ||
          reminder.title.toLowerCase().includes(search) ||
          reminder.message.toLowerCase().includes(search))
      );
    });

    return filtered.sort((a, b) => {
      if (this.sortOrder() === 'TITLE') return a.title.localeCompare(b.title);
      const order = a.reminderDateTime.localeCompare(b.reminderDateTime);
      return this.sortOrder() === 'EARLIEST' ? order : -order;
    });
  });

  readonly pageCount = computed(() =>
    Math.max(1, Math.ceil(this.filteredReminders().length / REMINDERS_PER_PAGE)),
  );
  readonly visibleReminders = computed(() =>
    this.filteredReminders().slice(
      this.page() * REMINDERS_PER_PAGE,
      (this.page() + 1) * REMINDERS_PER_PAGE,
    ),
  );

  constructor() {
    this.loadReminders();
  }

  loadReminders(): void {
    const requestId = ++this.requestId;
    this.loading.set(true);
    this.error.set(null);
    this.remindersApi
      .listAll()
      .pipe(
        finalize(() => {
          if (requestId === this.requestId) this.loading.set(false);
        }),
      )
      .subscribe({
        next: (reminders) => {
          if (requestId !== this.requestId) return;
          this.reminders.set(reminders);
          if (
            this.page() >=
            Math.max(1, Math.ceil(this.filteredReminders().length / REMINDERS_PER_PAGE))
          ) {
            this.page.set(0);
          }
        },
        error: (error: unknown) => {
          if (requestId !== this.requestId) return;
          this.error.set(readApiError(error, 'Could not load reminders. Please try again.'));
        },
      });
  }

  beginCreate(): void {
    this.editingReminder.set(null);
    this.editorError.set(null);
    this.editorOpen.set(true);
  }

  beginEdit(reminder: ReminderResponse): void {
    if (reminder.status !== 'PENDING') return;
    this.editingReminder.set(reminder);
    this.editorError.set(null);
    this.editorOpen.set(true);
  }

  saveReminder(event: { id: number | null; request: ReminderRequest }): void {
    this.saving.set(true);
    this.editorError.set(null);
    const operation =
      event.id === null
        ? this.remindersApi.create(event.request)
        : this.remindersApi.update(event.id, event.request);
    operation.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.editorOpen.set(false);
        this.success.set(event.id === null ? 'Reminder created.' : 'Reminder updated.');
        this.loadReminders();
      },
      error: (error: unknown) => {
        this.editorError.set(readApiError(error, 'Could not save this reminder.'));
      },
    });
  }

  async cancelReminder(reminder: ReminderResponse): Promise<void> {
    if (reminder.status !== 'PENDING') return;
    if (!(await this.confirmation.confirm({
      title: 'Cancel this reminder?',
      message: `Cancel the reminder "${reminder.title}"?`,
      confirmLabel: 'Yes, Cancel',
    }))) return;
    this.cancellingId.set(reminder.id);
    this.actionError.set(null);
    this.remindersApi
      .cancel(reminder.id)
      .pipe(finalize(() => this.cancellingId.set(null)))
      .subscribe({
        next: () => {
          this.success.set('Reminder cancelled.');
          this.loadReminders();
        },
        error: (error: unknown) => {
          this.actionError.set(readApiError(error, 'Could not cancel this reminder.'));
        },
      });
  }

  setStatusFilter(value: string): void {
    if (['', 'PENDING', 'SENT', 'CANCELLED'].includes(value)) {
      this.statusFilter.set(value as ReminderStatus | '');
      this.page.set(0);
    }
  }

  setSortOrder(value: string): void {
    if (['EARLIEST', 'LATEST', 'TITLE'].includes(value)) {
      this.sortOrder.set(value as 'EARLIEST' | 'LATEST' | 'TITLE');
      this.page.set(0);
    }
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

  formatDateTime(value: string): string {
    return this.regionalDate.formatDateTime(value, {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
    });
  }

  typeLabel(type: ReminderType): string {
    return {
      GENERAL: 'General',
      TASK: 'Task',
      GOAL: 'Goal',
      HABIT: 'Habit',
      CALENDAR: 'Calendar',
    }[type];
  }

  statusLabel(status: ReminderStatus): string {
    return { PENDING: 'Pending', SENT: 'Sent', CANCELLED: 'Cancelled' }[status];
  }

  clearMessages(): void {
    this.success.set(null);
    this.actionError.set(null);
  }

  private localDateTime(date: Date): string {
    const pad = (value: number) => String(value).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
  }
}

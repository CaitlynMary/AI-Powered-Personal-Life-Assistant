import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideDynamicIcon } from '@lucide/angular';
import { DashboardReminder } from '../../../../shared/models/dashboard.models';

@Component({
  selector: 'app-upcoming-reminders',
  standalone: true,
  imports: [LucideDynamicIcon, RouterLink],
  template: `
    <section class="ls-card flex h-full flex-col bg-[var(--ls-card-reminders)] p-5">
      <div class="mb-4 flex items-center justify-between gap-2">
        <h2 class="ls-section-title">Upcoming Reminders</h2>
        <a
          routerLink="/reminders"
          class="shrink-0 rounded-lg px-2 py-1 text-xs font-extrabold text-purple transition hover:bg-soft-pink/50"
        >
          View all
        </a>
      </div>

      @if (loading()) {
        <p class="flex-1 py-8 text-center text-sm font-medium text-ls-text/60" role="status">
          Loading reminders…
        </p>
      } @else if (hasError()) {
        <p class="flex-1 py-8 text-center text-sm font-medium text-ls-text/60">
          Reminder data is unavailable. Try refreshing the dashboard.
        </p>
      } @else if (reminders().length === 0) {
        <p class="flex-1 py-8 text-center text-sm font-medium text-ls-text/60">
          No upcoming reminders.
        </p>
      } @else {
        <ul class="flex flex-1 flex-col gap-3">
          @for (reminder of reminders(); track reminder.id) {
            <li
              class="flex items-center gap-3 rounded-2xl bg-ls-surface/55 px-3 py-3 transition hover:bg-ls-surface/80 dark:bg-plum/25 dark:hover:bg-plum/35"
            >
              <span
                class="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-plum/10 text-ls-text dark:bg-cream/10 dark:text-cream"
              >
                <svg [lucideIcon]="reminder.icon" [size]="18"></svg>
              </span>
              <div class="min-w-0">
                <p class="truncate text-sm font-bold text-ls-text dark:text-cream">
                  {{ reminder.title }}
                </p>
                <p class="text-xs font-medium text-ls-text/60 dark:text-cream/70">
                  {{ reminder.when }} · {{ reminder.status.toLowerCase() }}
                </p>
              </div>
            </li>
          }
        </ul>
      }
    </section>
  `,
})
export class UpcomingReminders {
  readonly reminders = input.required<DashboardReminder[]>();
  readonly loading = input(false);
  readonly hasError = input(false);
}

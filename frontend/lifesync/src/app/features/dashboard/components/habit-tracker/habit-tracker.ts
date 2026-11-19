import { Component, input } from '@angular/core';
import { LucideDynamicIcon } from '@lucide/angular';
import { DashboardHabit } from '../../../../shared/models/dashboard.models';

@Component({
  selector: 'app-habit-tracker',
  standalone: true,
  imports: [LucideDynamicIcon],
  template: `
    <section class="ls-card flex h-full flex-col bg-[var(--ls-card-habits)] p-5">
      <div class="mb-4 flex items-center justify-between">
        <h2 class="ls-section-title">Habit Tracker</h2>
        <span class="rounded-full bg-plum/10 px-2.5 py-1 text-xs font-bold text-ls-text dark:bg-cream/10 dark:text-cream"
          >This week</span
        >
      </div>

      @if (loading()) {
        <p class="flex-1 py-8 text-center text-sm font-medium text-ls-text/60" role="status">
          Loading habits…
        </p>
      } @else if (hasError()) {
        <p class="flex-1 py-8 text-center text-sm font-medium text-ls-text/60">
          Habit data is unavailable. Try refreshing the dashboard.
        </p>
      } @else if (habits().length === 0) {
        <p class="flex-1 py-8 text-center text-sm font-medium text-ls-text/60">
          No active habits yet.
        </p>
      } @else {
        <ul class="flex flex-1 flex-col gap-3">
          @for (habit of habits(); track habit.id) {
            <li class="rounded-2xl bg-ls-surface/50 p-3 dark:bg-plum/20">
              <div class="mb-2 flex items-center justify-between gap-2">
                <p class="text-sm font-bold text-ls-text dark:text-cream">{{ habit.name }}</p>
                <p class="text-xs font-semibold text-ls-text/60 dark:text-cream/70">
                  {{ habit.streak }} day{{ habit.streak === 1 ? '' : 's' }} streak
                </p>
              </div>
              <div class="flex flex-wrap gap-1.5">
                @for (day of habit.days; track $index) {
                  <span
                    class="flex h-8 w-8 items-center justify-center rounded-full text-[10px] font-bold"
                    [class]="
                      day.completed
                        ? 'bg-aqua text-plum'
                        : 'bg-plum/10 text-ls-text/50 dark:bg-cream/10 dark:text-cream/50'
                    "
                    [attr.title]="day.label"
                  >
                    @if (day.completed) {
                      <svg lucideIcon="check" [size]="14" strokeWidth="3"></svg>
                    } @else {
                      {{ day.label }}
                    }
                  </span>
                }
              </div>
            </li>
          }
          </ul>
      }
    </section>
  `,
})
export class HabitTracker {
  readonly habits = input.required<DashboardHabit[]>();
  readonly loading = input(false);
  readonly hasError = input(false);
}

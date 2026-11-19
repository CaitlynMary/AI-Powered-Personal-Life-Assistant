import { Component, input } from '@angular/core';
import { LucideDynamicIcon } from '@lucide/angular';
import { DashboardGoal } from '../../../../shared/models/dashboard.models';

@Component({
  selector: 'app-goal-progress',
  standalone: true,
  imports: [LucideDynamicIcon],
  template: `
    <section class="ls-card flex h-full flex-col bg-[var(--ls-card-goals)] p-5">
      <h2 class="ls-section-title mb-4">Goal Progress</h2>

      @if (loading()) {
        <p class="flex-1 py-8 text-center text-sm font-medium text-ls-text/60" role="status">
          Loading goals…
        </p>
      } @else if (hasError()) {
        <p class="flex-1 py-8 text-center text-sm font-medium text-ls-text/60">
          Goal data is unavailable. Try refreshing the dashboard.
        </p>
      } @else if (goals().length === 0) {
        <p class="flex-1 py-8 text-center text-sm font-medium text-ls-text/60">
          No goals yet. Your progress will appear here.
        </p>
      } @else {
        <ul class="flex flex-1 flex-col gap-4">
          @for (goal of goals(); track goal.id) {
            <li class="rounded-2xl bg-ls-surface/45 p-3 dark:bg-plum/25">
              <div class="mb-2 flex items-center gap-3">
                <span
                  class="flex h-10 w-10 items-center justify-center rounded-xl bg-plum/10 text-ls-text dark:bg-cream/10 dark:text-cream"
                >
                  <svg [lucideIcon]="goal.icon" [size]="18"></svg>
                </span>
                <div class="min-w-0 flex-1">
                  <p class="truncate text-sm font-bold text-plum dark:text-ls-text-muted">
                    {{ goal.title }}
                  </p>
                  <p
                    class="text-xs font-bold capitalize text-plum dark:text-ls-text-muted"
                    [class.text-burgundy]="goal.status === 'OVERDUE'"
                  >
                    {{ goal.progress }}% · {{ goal.status.toLowerCase().replace('_', ' ') }}
                  </p>
                </div>
              </div>
              <div class="h-2.5 overflow-hidden rounded-full bg-plum/10 dark:bg-cream/15">
                <div
                  class="h-full rounded-full bg-lime transition-all duration-500"
                  [style.width.%]="goal.progress"
                ></div>
              </div>
            </li>
          }
        </ul>
      }
    </section>
  `,
})
export class GoalProgress {
  readonly goals = input.required<DashboardGoal[]>();
  readonly loading = input(false);
  readonly hasError = input(false);
}

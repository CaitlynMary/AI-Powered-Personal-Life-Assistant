import { NgClass } from '@angular/common';
import { Component, input } from '@angular/core';
import { LucideDynamicIcon } from '@lucide/angular';
import { DashboardStat } from '../../models/dashboard.models';

const TONE_BG: Record<DashboardStat['tone'], string> = {
  tasks: 'bg-[var(--ls-stat-tasks)]',
  goals: 'bg-[var(--ls-stat-goals)]',
  habits: 'bg-[var(--ls-stat-habits)]',
  reminders: 'bg-[var(--ls-stat-reminders)]',
};

const STAT_BG: Record<string, string> = {
  'active-tasks': 'bg-[var(--ls-stat-active-tasks)]',
  'pending-tasks': 'bg-[var(--ls-stat-pending-tasks)]',
  'in-progress-tasks': 'bg-[var(--ls-stat-in-progress-tasks)]',
  'completed-tasks': 'bg-[var(--ls-stat-completed-tasks)]',
  'overdue-tasks': 'bg-[var(--ls-stat-overdue-tasks)]',
  'missed-tasks': 'bg-[var(--ls-stat-missed-tasks)]',
  'active-goals': 'bg-[var(--ls-stat-active-goals)]',
  'habit-streak': 'bg-[var(--ls-stat-habit-streak)]',
  'upcoming-reminders': 'bg-[var(--ls-stat-upcoming-reminders)]',
};

@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [NgClass, LucideDynamicIcon],
  template: `
    <article
      class="ls-card flex items-center gap-4 p-4 transition duration-200 hover:-translate-y-0.5 hover:shadow-soft"
      [ngClass]="toneClass()"
    >
      <div
        class="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-plum/10 text-ls-text dark:bg-cream/15 dark:text-cream"
      >
        <svg [lucideIcon]="stat().icon" [size]="22" strokeWidth="2.25"></svg>
      </div>
      <div class="min-w-0">
        <p class="text-2xl font-extrabold tracking-tight text-plum dark:text-ls-text-muted">
          {{ stat().value }}
        </p>
        <p class="truncate text-sm font-semibold text-plum dark:text-ls-text-muted">
          {{ stat().label }}
        </p>
      </div>
    </article>
  `,
})
export class StatCard {
  readonly stat = input.required<DashboardStat>();

  toneClass(): string {
    return STAT_BG[this.stat().id] ?? TONE_BG[this.stat().tone];
  }
}

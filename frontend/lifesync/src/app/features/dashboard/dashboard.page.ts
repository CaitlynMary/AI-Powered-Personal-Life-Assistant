import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { readApiError } from '../../core/auth/auth-error';
import { AuthService } from '../../core/auth/auth.service';
import { RegionalDateService } from '../../core/services/regional-date.service';
import { StatCard } from '../../shared/components/stat-card/stat-card';
import { AiAssistantCard } from './components/ai-assistant-card/ai-assistant-card';
import { GoalProgress } from './components/goal-progress/goal-progress';
import { HabitTracker } from './components/habit-tracker/habit-tracker';
import { MotivationalQuote } from './components/motivational-quote/motivational-quote';
import { TodaysTasks } from './components/todays-tasks/todays-tasks';
import { UpcomingReminders } from './components/upcoming-reminders/upcoming-reminders';
import { WelcomeBanner } from './components/welcome-banner/welcome-banner';
import { DashboardApiService } from './data/dashboard-api.service';
import { getDailyInspiration } from './data/daily-inspiration';
import { DASHBOARD_MOCK } from './data/dashboard.mock';
import { DashboardData } from './data/dashboard-api.models';
import { createDailyBriefing } from '../ai-assistant/data/ai-assistant.models';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [
    StatCard,
    WelcomeBanner,
    TodaysTasks,
    GoalProgress,
    AiAssistantCard,
    HabitTracker,
    UpcomingReminders,
    MotivationalQuote,
  ],
  templateUrl: './dashboard.page.html',
})
export class DashboardPage {
  private readonly auth = inject(AuthService);
  private readonly regionalDate = inject(RegionalDateService);
  private readonly dashboardApi = inject(DashboardApiService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private greetingTimer: ReturnType<typeof setTimeout> | null = null;

  readonly presentation = DASHBOARD_MOCK;
  private readonly localTime = signal(new Date());
  readonly data = signal<DashboardData | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly taskActionError = signal<string | null>(null);
  readonly updatingTaskId = signal<string | null>(null);

  readonly stats = computed(() => this.data()?.stats ?? []);
  readonly tasks = computed(() => this.data()?.tasks ?? []);
  readonly goals = computed(() => this.data()?.goals ?? []);
  readonly habits = computed(() => this.data()?.habits ?? []);
  readonly reminders = computed(() => this.data()?.reminders ?? []);
  readonly briefing = computed(() => {
    const data = this.data();
    return data ? createDailyBriefing(data) : null;
  });
  readonly dailyInspiration = computed(() => getDailyInspiration(this.localTime()));
  readonly greeting = computed(() => {
    const fullName = this.auth.currentUserProfile()?.fullName.trim();
    const firstName = fullName?.split(/\s+/)[0] || 'there';
    const hour = this.localTime().getHours();

    if (hour >= 5 && hour < 12) {
      return `Good Morning, ${firstName}! ☀️`;
    }
    if (hour >= 12 && hour < 17) {
      return `Good Afternoon, ${firstName}! 🌤️`;
    }
    return `Good Evening, ${firstName}! 🌙`;
  });
  readonly dateLabel = computed(() =>
    this.regionalDate.formatDateOnly(new Date(), {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    }),
  );

  constructor() {
    this.destroyRef.onDestroy(() => {
      if (this.greetingTimer !== null) {
        clearTimeout(this.greetingTimer);
      }
    });
    this.scheduleGreetingUpdate();
    this.loadDashboard();
  }

  private scheduleGreetingUpdate(): void {
    const now = new Date();
    const next = new Date(now);
    const hour = now.getHours();
    const nextBoundaryHour = [0, 5, 12, 17].find((boundary) => boundary > hour) ?? 0;

    if (nextBoundaryHour <= hour) {
      next.setDate(next.getDate() + 1);
    }
    next.setHours(nextBoundaryHour, 0, 0, 0);

    this.greetingTimer = setTimeout(() => {
      this.localTime.set(new Date());
      this.scheduleGreetingUpdate();
    }, next.getTime() - now.getTime());
  }

  loadDashboard(showLoading = true): void {
    if (showLoading) {
      this.loading.set(true);
      this.data.set(null);
    }
    this.error.set(null);

    this.dashboardApi
      .loadDashboard()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (data) => this.data.set(data),
        error: (error: unknown) => {
          this.data.set(null);
          this.error.set(readApiError(error, 'Could not load dashboard data. Please try again.'));
        },
      });
  }

  onToggleTask(taskId: string): void {
    const task = this.tasks().find((item) => item.id === taskId);
    if (!task || this.updatingTaskId()) {
      return;
    }

    this.taskActionError.set(null);
    this.updatingTaskId.set(taskId);
    this.dashboardApi
      .setTaskCompleted(taskId, !task.completed)
      .pipe(finalize(() => this.updatingTaskId.set(null)))
      .subscribe({
        next: () => this.loadDashboard(false),
        error: (error: unknown) => {
          this.taskActionError.set(
            readApiError(error, 'Could not update this task. Please try again.'),
          );
        },
      });
  }

  onAddTask(): void {
    void this.router.navigate(['/tasks'], {
      queryParams: { openCreateTask: 'true' },
    });
  }

  onAiSuggestion(_id: string): void {
    void this.router.navigate(['/ai-assistant']);
  }

  onChatWithAi(): void {
    void this.router.navigate(['/ai-assistant']);
  }
}

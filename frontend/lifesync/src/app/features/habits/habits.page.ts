import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize, forkJoin, map, of, switchMap } from 'rxjs';
import { readApiError } from '../../core/auth/auth-error';
import { RegionalDateService } from '../../core/services/regional-date.service';
import { ConfirmationDialogService } from '../../shared/confirmation-dialog/confirmation-dialog.service';
import { HabitApiService } from './data/habit-api.service';
import {
  HabitCalendarDay,
  HabitCalendarResponse,
  HabitFrequency,
  HabitLogResponse,
  HabitRequest,
  HabitResponse,
  HabitStatisticsResponse,
  HabitStreakResponse,
} from './data/habit.models';
import { HabitLogApiService } from './data/habit-log-api.service';
import { HabitForm } from './habit-form/habit-form';

interface HabitDetails {
  habit: HabitResponse;
  logs: HabitLogResponse[];
  streak: HabitStreakResponse;
}

@Component({
  selector: 'app-habits-page',
  standalone: true,
  imports: [FormsModule, HabitForm],
  templateUrl: './habits.page.html',
})
export class HabitsPage {
  private readonly regionalDate = inject(RegionalDateService);
  private readonly habitsApi = inject(HabitApiService);
  private readonly logsApi = inject(HabitLogApiService);
  private readonly confirmation = inject(ConfirmationDialogService);
  private dataRequestId = 0;
  private calendarRequestId = 0;

  readonly habitDetails = signal<HabitDetails[]>([]);
  readonly statistics = signal<HabitStatisticsResponse | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly saving = signal(false);
  readonly deletingId = signal<number | null>(null);
  readonly deactivatingId = signal<number | null>(null);
  readonly completingId = signal<number | null>(null);
  readonly habitFormOpen = signal(false);
  readonly selectedHabit = signal<HabitResponse | null>(null);
  readonly formError = signal<string | null>(null);
  readonly searchText = signal('');
  readonly frequencyFilter = signal<HabitFrequency | ''>('');
  readonly activeFilter = signal<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');
  readonly sortBy = signal<'NAME' | 'STREAK'>('NAME');
  readonly selectedCalendarHabitId = signal<number | null>(null);
  readonly calendar = signal<HabitCalendarResponse | null>(null);
  readonly calendarLoading = signal(false);
  readonly calendarError = signal<string | null>(null);
  readonly selectedCalendarDate = signal<string | null>(null);
  readonly calendarYear = signal(new Date().getFullYear());
  readonly calendarMonth = signal(new Date().getMonth() + 1);
  readonly visibleHabits = computed(() => {
    const search = this.searchText().trim().toLowerCase();
    const filtered = this.habitDetails().filter(({ habit }) => {
      const matchesSearch =
        !search ||
        habit.name.toLowerCase().includes(search) ||
        (habit.description ?? '').toLowerCase().includes(search);
      return (
        matchesSearch &&
        (!this.frequencyFilter() || habit.frequency === this.frequencyFilter()) &&
        (this.activeFilter() === 'ALL' ||
          (this.activeFilter() === 'ACTIVE' ? habit.active : !habit.active))
      );
    });
    return filtered.sort((a, b) =>
      this.sortBy() === 'NAME'
        ? a.habit.name.localeCompare(b.habit.name)
        : b.streak.currentStreak - a.streak.currentStreak,
    );
  });
  readonly selectedCalendarHabit = computed(
    () =>
      this.habitDetails().find(({ habit }) => habit.id === this.selectedCalendarHabitId())?.habit ??
      null,
  );
  readonly calendarGrid = computed(() => {
    const calendar = this.calendar();
    if (!calendar) return [];
    const firstWeekday = new Date(calendar.year, calendar.month - 1, 1).getDay();
    return [...Array<HabitCalendarDay | null>(firstWeekday).fill(null), ...calendar.days];
  });
  readonly selectedDay = computed(
    () => this.calendar()?.days.find((day) => day.date === this.selectedCalendarDate()) ?? null,
  );

  constructor() {
    this.loadHabits();
  }

  loadHabits(): void {
    const requestId = ++this.dataRequestId;
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      habits: this.habitsApi.list(),
      statistics: this.habitsApi.statistics(),
    })
      .pipe(
        switchMap(({ habits, statistics }) =>
          habits.length
            ? forkJoin(
                habits.map((habit) =>
                  forkJoin({
                    logs: this.logsApi.list(habit.id),
                    streak: this.logsApi.streak(habit.id),
                  }).pipe(map(({ logs, streak }) => ({ habit, logs, streak }))),
                ),
              ).pipe(map((details) => ({ details, statistics })))
            : of({ details: [] as HabitDetails[], statistics }),
        ),
        finalize(() => {
          if (requestId === this.dataRequestId) this.loading.set(false);
        }),
      )
      .subscribe({
        next: ({ statistics, details }) => {
          if (requestId !== this.dataRequestId) return;
          this.statistics.set(statistics);
          this.setHabitDetails(details);
        },
        error: (error: unknown) => {
          if (requestId !== this.dataRequestId) return;
          this.error.set(readApiError(error, 'Could not load habits. Please try again.'));
        },
      });
  }

  private setHabitDetails(details: HabitDetails[]): void {
    const priorSelectedId = this.selectedCalendarHabitId();
    this.habitDetails.set(details);
    const hasPrior = details.some(({ habit }) => habit.id === priorSelectedId);
    const selected = hasPrior
      ? priorSelectedId
      : (details.find(({ habit }) => habit.active)?.habit.id ?? details[0]?.habit.id ?? null);
    this.selectedCalendarHabitId.set(selected);
    this.loading.set(false);
    if (selected !== null) this.loadCalendar();
    else {
      this.calendar.set(null);
      this.calendarError.set(null);
    }
  }

  loadCalendar(): void {
    const habitId = this.selectedCalendarHabitId();
    if (habitId === null) {
      this.calendarRequestId += 1;
      this.calendar.set(null);
      this.calendarLoading.set(false);
      this.calendarError.set(null);
      return;
    }
    const requestId = ++this.calendarRequestId;
    this.calendar.set(null);
    this.calendarLoading.set(true);
    this.calendarError.set(null);
    this.logsApi
      .calendar(habitId, this.calendarYear(), this.calendarMonth())
      .pipe(
        finalize(() => {
          if (requestId === this.calendarRequestId) this.calendarLoading.set(false);
        }),
      )
      .subscribe({
        next: (calendar) => {
          if (requestId !== this.calendarRequestId) return;
          this.calendar.set(calendar);
          if (!calendar.days.some((day) => day.date === this.selectedCalendarDate())) {
            this.selectedCalendarDate.set(null);
          }
        },
        error: (error: unknown) => {
          if (requestId !== this.calendarRequestId) return;
          this.calendarError.set(readApiError(error, 'Could not load this habit calendar.'));
        },
      });
  }

  setCalendarHabit(value: string): void {
    const id = Number(value);
    if (!Number.isSafeInteger(id) || id <= 0) return;
    this.selectedCalendarHabitId.set(id);
    this.selectedCalendarDate.set(null);
    this.loadCalendar();
  }

  changeMonth(offset: number): void {
    const date = new Date(this.calendarYear(), this.calendarMonth() - 1 + offset, 1);
    this.calendarYear.set(date.getFullYear());
    this.calendarMonth.set(date.getMonth() + 1);
    this.selectedCalendarDate.set(null);
    this.loadCalendar();
  }

  monthLabel(): string {
    return this.regionalDate.formatDateOnly(
      new Date(this.calendarYear(), this.calendarMonth() - 1, 1),
      { month: 'long', year: 'numeric' },
    );
  }

  beginCreate(): void {
    this.selectedHabit.set(null);
    this.formError.set(null);
    this.habitFormOpen.set(true);
  }

  beginEdit(habit: HabitResponse): void {
    this.selectedHabit.set(habit);
    this.formError.set(null);
    this.habitFormOpen.set(true);
  }

  saveHabit(event: { id: number | null; request: HabitRequest }): void {
    this.saving.set(true);
    this.formError.set(null);
    const operation =
      event.id === null
        ? this.habitsApi.create(event.request)
        : this.habitsApi.update(event.id, event.request);
    operation.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.habitFormOpen.set(false);
        this.success.set(event.id === null ? 'Habit created.' : 'Habit updated.');
        this.loadHabits();
      },
      error: (error: unknown) => {
        this.formError.set(
          readApiError(
            error,
            'Could not save this habit. Its date range must include all existing history.',
          ),
        );
      },
    });
  }

  async deleteHabit(habit: HabitResponse): Promise<void> {
    if (
      !(await this.confirmation.confirm({
        title: 'Delete this habit?',
        message: `Delete "${habit.name}" and all of its habit history? This cannot be undone.`,
        confirmLabel: 'Yes, Delete',
      }))
    )
      return;
    this.deletingId.set(habit.id);
    this.actionError.set(null);
    this.habitsApi
      .delete(habit.id)
      .pipe(finalize(() => this.deletingId.set(null)))
      .subscribe({
        next: () => {
          this.success.set('Habit and its history deleted.');
          this.loadHabits();
        },
        error: (error: unknown) => {
          this.actionError.set(readApiError(error, 'Could not delete this habit.'));
        },
      });
  }

  async deactivateHabit(habit: HabitResponse): Promise<void> {
    if (
      !(await this.confirmation.confirm({
        title: 'Deactivate this habit?',
        message: `Deactivate "${habit.name}"? It will remain visible but cannot be logged.`,
        confirmLabel: 'Yes, Deactivate',
      }))
    )
      return;
    this.deactivatingId.set(habit.id);
    this.actionError.set(null);
    this.habitsApi
      .deactivate(habit.id)
      .pipe(finalize(() => this.deactivatingId.set(null)))
      .subscribe({
        next: () => {
          this.success.set('Habit deactivated.');
          this.loadHabits();
        },
        error: (error: unknown) => {
          this.actionError.set(readApiError(error, 'Could not deactivate this habit.'));
        },
      });
  }

  completeToday(habit: HabitResponse): void {
    this.toggleCompletionForDate(habit, this.today());
  }

  completeSelectedDay(): void {
    const habit = this.selectedCalendarHabit();
    const day = this.selectedDay();
    if (!habit || !day) return;
    this.toggleCompletionForDate(habit, day.date);
  }

  private toggleCompletionForDate(habit: HabitResponse, date: string): void {
    if (this.completingId() !== null || !this.canCompleteDate(habit, date)) return;
    const completed = this.isCompleted(habit.id, date);
    this.completingId.set(habit.id);
    this.actionError.set(null);
    this.logsApi
      .complete(habit.id, { completionDate: date, completed: !completed })
      .pipe(finalize(() => this.completingId.set(null)))
      .subscribe({
        next: () => {
          this.success.set(
            completed
              ? `${habit.name} completion was undone for ${this.formatDate(date)}.`
              : `${habit.name} completed for ${this.formatDate(date)}.`,
          );
          this.loadHabits();
        },
        error: (error: unknown) => {
          this.actionError.set(readApiError(error, 'Could not update this habit completion.'));
          this.loadHabits();
        },
      });
  }

  todayLog(habitId: number): HabitLogResponse | undefined {
    return this.logForDate(habitId, this.today());
  }

  private logForDate(habitId: number, date: string): HabitLogResponse | undefined {
    return this.habitDetails()
      .find(({ habit }) => habit.id === habitId)
      ?.logs.find((log) => log.completionDate === date);
  }

  private isCompleted(habitId: number, date: string): boolean {
    return this.logForDate(habitId, date)?.completed === true;
  }

  today(): string {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
  }

  canCompleteDate(habit: HabitResponse, date: string): boolean {
    if (
      !habit.active ||
      date > this.today() ||
      date < habit.startDate ||
      (habit.endDate !== null && date > habit.endDate)
    ) {
      return false;
    }
    if (this.isCompleted(habit.id, date)) return true;
    const completedLogs =
      this.habitDetails()
        .find(({ habit: item }) => item.id === habit.id)
        ?.logs.filter((log) => log.completed) ?? [];
    if (habit.frequency === 'DAILY') return true;
    if (habit.frequency === 'WEEKLY') {
      const start = this.weekStart(date);
      const end = this.addDays(start, 6);
      const completedThisWeek = completedLogs.filter(
        (log) => log.completionDate >= start && log.completionDate <= end,
      ).length;
      return completedThisWeek < habit.targetDaysPerWeek;
    }
    return !completedLogs.some((log) => log.completionDate.slice(0, 7) === date.slice(0, 7));
  }

  todayAvailabilityMessage(habit: HabitResponse): string {
    const date = this.today();
    if (!habit.active) return 'Deactivated habits cannot be logged.';
    if (date < habit.startDate || (habit.endDate !== null && date > habit.endDate)) {
      return 'Today is outside this habit’s date range.';
    }
    if (habit.frequency === 'WEEKLY') return 'This week’s completion target is already met.';
    if (habit.frequency === 'MONTHLY') return 'This month’s completion is already recorded.';
    return 'This date is not eligible for completion.';
  }

  calendarDayStatus(day: HabitCalendarDay): string {
    if (day.completed) return 'Completed';
    const habit = this.selectedCalendarHabit();
    if (
      !habit ||
      day.date < habit.startDate ||
      (habit.endDate !== null && day.date > habit.endDate)
    ) {
      return 'Outside habit date range';
    }
    if (day.date > this.today()) return 'Future date';
    if (!habit.active) return 'Inactive habit';
    if (day.eligible) return 'Eligible but not completed';
    if (habit.frequency === 'WEEKLY') return 'Weekly target already met';
    if (habit.frequency === 'MONTHLY') return 'Monthly completion already recorded';
    return 'Not eligible';
  }

  formatDate(date: string): string {
    return this.regionalDate.formatDateOnly(date, { dateStyle: 'medium' });
  }

  private weekStart(date: string): string {
    const [year, month, day] = date.split('-').map(Number);
    const localDate = new Date(year, month - 1, day);
    const offset = (localDate.getDay() - this.regionalDate.weekStartIndex() + 7) % 7;
    localDate.setDate(localDate.getDate() - offset);
    return this.dateString(localDate);
  }

  private addDays(date: string, days: number): string {
    const [year, month, day] = date.split('-').map(Number);
    const localDate = new Date(year, month - 1, day);
    localDate.setDate(localDate.getDate() + days);
    return this.dateString(localDate);
  }

  private dateString(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }

  dayClass(day: HabitCalendarDay): string {
    if (day.completed) return 'bg-lime text-plum ring-2 ring-mint';
    const habit = this.selectedCalendarHabit();
    if (
      !habit ||
      day.date < habit.startDate ||
      (habit.endDate !== null && day.date > habit.endDate)
    ) {
      return 'bg-ls-bg text-ls-text-muted/50';
    }
    if (day.date > this.today()) return 'bg-purple/10 text-ls-text-muted';
    if (!day.eligible) return 'bg-ls-bg text-ls-text-muted';
    return 'bg-ls-surface-2/70 text-ls-text';
  }

  frequencyLabel(frequency: HabitFrequency): string {
    return { DAILY: 'Daily', WEEKLY: 'Weekly', MONTHLY: 'Monthly' }[frequency];
  }

  clearSuccess(): void {
    this.success.set(null);
  }
}

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../core/api.config';
import { ConfirmationDialogService } from '../../shared/confirmation-dialog/confirmation-dialog.service';
import { HabitCalendarResponse, HabitLogResponse, HabitResponse } from './data/habit.models';
import { HabitsPage } from './habits.page';

const habit: HabitResponse = {
  id: 4,
  name: 'Walk',
  description: 'Take a walk outside',
  frequency: 'DAILY',
  targetDaysPerWeek: 7,
  startDate: localDate(-30),
  endDate: null,
  active: true,
  createdAt: '2026-01-01T10:00:00',
  updatedAt: '2026-01-01T10:00:00',
};

function localDate(offsetDays = 0): string {
  const date = new Date();
  date.setDate(date.getDate() + offsetDays);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function logForToday(completed: boolean): HabitLogResponse {
  const date = localDate();
  return {
    id: 18,
    habitId: habit.id,
    completionDate: date,
    completed,
    notes: null,
    createdAt: `${date}T10:00:00`,
  };
}

function calendarResponse(
  year: number,
  month: number,
  completedDates: string[] | null = null,
): HabitCalendarResponse {
  const daysCount = new Date(year, month, 0).getDate();
  return {
    habitId: habit.id,
    year,
    month,
    completedDays: completedDates === null ? 1 : completedDates.length,
    eligibleDays: Math.min(daysCount, new Date().getDate()),
    completionPercentage: 25,
    days: Array.from({ length: daysCount }, (_, index) => {
      const date = `${year}-${String(month).padStart(2, '0')}-${String(index + 1).padStart(2, '0')}`;
      return {
        date,
        eligible: true,
        completed: completedDates === null ? index === 0 : completedDates.includes(date),
        notes: completedDates === null && index === 0 ? 'First day' : null,
      };
    }),
  };
}

describe('HabitsPage', () => {
  let fixture: ComponentFixture<HabitsPage>;
  let component: HabitsPage;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    fixture = TestBed.createComponent(HabitsPage);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function flushInitial(
    habits: HabitResponse[],
    logsByHabit: Record<number, HabitLogResponse[]> = {},
  ): void {
    fixture.detectChanges();
    http.expectOne(`${API_BASE_URL}/api/habits`).flush(habits);
    http.expectOne(`${API_BASE_URL}/api/habits/statistics`).flush({
      totalHabits: habits.length,
      activeHabits: habits.filter((item) => item.active).length,
      totalCompletedDays: 8,
      completionsThisWeek: 3,
      completionsThisMonth: 6,
      weeklyConsistencyPercentage: 75,
      monthlyConsistencyPercentage: 60,
    });
    for (const item of habits) {
      http
        .expectOne(`${API_BASE_URL}/api/habits/${item.id}/logs`)
        .flush(logsByHabit[item.id] ?? []);
      http.expectOne(`${API_BASE_URL}/api/habits/${item.id}/streak`).flush({
        currentStreak: 3,
        longestStreak: 7,
        totalCompletedDays: 8,
        completionPercentage: 66.67,
        periodUnit:
          item.frequency === 'DAILY' ? 'days' : item.frequency === 'WEEKLY' ? 'weeks' : 'months',
      });
    }
    if (habits.length) {
      const now = new Date();
      const calendarRequest = http.expectOne(
        (request) => request.url === `${API_BASE_URL}/api/habits/${habits[0].id}/calendar`,
      );
      expect(calendarRequest.request.params.get('year')).toBe(String(now.getFullYear()));
      expect(calendarRequest.request.params.get('month')).toBe(String(now.getMonth() + 1));
      calendarRequest.flush(calendarResponse(now.getFullYear(), now.getMonth() + 1));
    }
    fixture.detectChanges();
  }

  it('shows loading and a friendly empty state for no habits', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Loading your habits');
    http.expectOne(`${API_BASE_URL}/api/habits`).flush([]);
    http.expectOne(`${API_BASE_URL}/api/habits/statistics`).flush({
      totalHabits: 0,
      activeHabits: 0,
      totalCompletedDays: 0,
      completionsThisWeek: 0,
      completionsThisMonth: 0,
      weeklyConsistencyPercentage: 0,
      monthlyConsistencyPercentage: 0,
    });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No habits yet');
    expect(fixture.nativeElement.textContent).toContain('Create a habit to view its calendar');
  });

  it('displays backend habit details, statistics, and streak values', () => {
    flushInitial([habit], { [habit.id]: [logForToday(true)] });

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Walk');
    expect(text).toContain('Take a walk outside');
    expect(text).toContain('3 days');
    expect(text).toContain('7 days');
    expect(text).toContain('66.67%');
    expect(text).toContain('75%');
    expect(text).toContain('Completed · Undo');
  });

  it('searches habit names and descriptions case-insensitively with partial matches', () => {
    flushInitial([habit]);

    component.searchText.set('  WALK  ');
    expect(component.visibleHabits().map(({ habit: item }) => item.id)).toEqual([habit.id]);
    component.searchText.set('K OUT');
    expect(component.visibleHabits().map(({ habit: item }) => item.id)).toEqual([habit.id]);
    component.searchText.set('no-such-habit');
    expect(component.visibleHabits()).toEqual([]);
    component.searchText.set('');
    expect(component.visibleHabits().map(({ habit: item }) => item.id)).toEqual([habit.id]);
  });

  it('sends an undo request for an already completed date', () => {
    flushInitial([habit], { [habit.id]: [logForToday(true)] });

    component.completeToday(habit);

    const request = http.expectOne(`${API_BASE_URL}/api/habits/${habit.id}/complete`);
    expect(request.request.body).toEqual({ completionDate: component.today(), completed: false });
  });

  it('refreshes persisted logs, statistics, streak, and calendar after undoing completion', () => {
    flushInitial([habit], { [habit.id]: [logForToday(true)] });

    component.completeToday(habit);
    http
      .expectOne(`${API_BASE_URL}/api/habits/${habit.id}/complete`)
      .flush({ ...logForToday(false), id: 18 });

    http.expectOne(`${API_BASE_URL}/api/habits`).flush([habit]);
    http.expectOne(`${API_BASE_URL}/api/habits/statistics`).flush({
      totalHabits: 1,
      activeHabits: 1,
      totalCompletedDays: 7,
      completionsThisWeek: 2,
      completionsThisMonth: 5,
      weeklyConsistencyPercentage: 50,
      monthlyConsistencyPercentage: 50,
    });
    http
      .expectOne(`${API_BASE_URL}/api/habits/${habit.id}/logs`)
      .flush([{ ...logForToday(false), id: 18 }]);
    http.expectOne(`${API_BASE_URL}/api/habits/${habit.id}/streak`).flush({
      currentStreak: 0,
      longestStreak: 7,
      totalCompletedDays: 7,
      completionPercentage: 50,
      periodUnit: 'days',
    });
    const now = new Date();
    http
      .expectOne((request) => request.url === `${API_BASE_URL}/api/habits/${habit.id}/calendar`)
      .flush(calendarResponse(now.getFullYear(), now.getMonth() + 1, []));
    fixture.detectChanges();

    expect(component.todayLog(habit.id)?.completed).toBe(false);
    expect(component.statistics()?.totalCompletedDays).toBe(7);
    expect(component.habitDetails()[0].streak.currentStreak).toBe(0);
    expect(component.calendar()?.days[new Date().getDate() - 1].completed).toBe(false);
    expect(component.success()).toContain('completion was undone');
  });

  it('records completion once and reloads logs, streak, statistics, and calendar', () => {
    flushInitial([habit]);

    component.completeToday(habit);
    component.completeToday(habit);
    const completionRequest = http.expectOne(`${API_BASE_URL}/api/habits/${habit.id}/complete`);
    expect(completionRequest.request.method).toBe('PATCH');
    expect(completionRequest.request.body).toEqual({
      completionDate: component.today(),
      completed: true,
    });
    completionRequest.flush(logForToday(true));

    http.expectOne(`${API_BASE_URL}/api/habits`).flush([habit]);
    http.expectOne(`${API_BASE_URL}/api/habits/statistics`).flush({
      totalHabits: 1,
      activeHabits: 1,
      totalCompletedDays: 9,
      completionsThisWeek: 4,
      completionsThisMonth: 7,
      weeklyConsistencyPercentage: 100,
      monthlyConsistencyPercentage: 70,
    });
    http.expectOne(`${API_BASE_URL}/api/habits/${habit.id}/logs`).flush([logForToday(true)]);
    http.expectOne(`${API_BASE_URL}/api/habits/${habit.id}/streak`).flush({
      currentStreak: 4,
      longestStreak: 7,
      totalCompletedDays: 9,
      completionPercentage: 70,
      periodUnit: 'days',
    });
    const now = new Date();
    http
      .expectOne((request) => request.url === `${API_BASE_URL}/api/habits/${habit.id}/calendar`)
      .flush(calendarResponse(now.getFullYear(), now.getMonth() + 1));
    fixture.detectChanges();

    expect(component.todayLog(habit.id)?.completed).toBe(true);
    expect(component.habitDetails()[0].streak.currentStreak).toBe(4);
    expect(component.statistics()?.totalCompletedDays).toBe(9);
    expect(component.success()).toBe(
      `Walk completed for ${component.formatDate(component.today())}.`,
    );
  });

  it('completes the date selected in the calendar', () => {
    flushInitial([habit]);
    const pastDate = localDate(-1);
    if (
      pastDate.slice(0, 7) !==
      `${component.calendarYear()}-${String(component.calendarMonth()).padStart(2, '0')}`
    ) {
      component.changeMonth(-1);
      http
        .expectOne((request) => request.url === `${API_BASE_URL}/api/habits/${habit.id}/calendar`)
        .flush(calendarResponse(component.calendarYear(), component.calendarMonth(), []));
    }

    component.selectedCalendarDate.set(pastDate);
    component.completeSelectedDay();
    const completionRequest = http.expectOne(`${API_BASE_URL}/api/habits/${habit.id}/complete`);
    expect(completionRequest.request.body).toEqual({ completionDate: pastDate, completed: true });
    completionRequest.flush({ ...logForToday(true), completionDate: pastDate });

    http.expectOne(`${API_BASE_URL}/api/habits`).flush([habit]);
    http.expectOne(`${API_BASE_URL}/api/habits/statistics`).flush({
      totalHabits: 1,
      activeHabits: 1,
      totalCompletedDays: 9,
      completionsThisWeek: 4,
      completionsThisMonth: 7,
      weeklyConsistencyPercentage: 100,
      monthlyConsistencyPercentage: 70,
    });
    http
      .expectOne(`${API_BASE_URL}/api/habits/${habit.id}/logs`)
      .flush([{ ...logForToday(true), completionDate: pastDate }]);
    http.expectOne(`${API_BASE_URL}/api/habits/${habit.id}/streak`).flush({
      currentStreak: 4,
      longestStreak: 7,
      totalCompletedDays: 9,
      completionPercentage: 70,
      periodUnit: 'days',
    });
    http
      .expectOne((request) => request.url === `${API_BASE_URL}/api/habits/${habit.id}/calendar`)
      .flush(calendarResponse(component.calendarYear(), component.calendarMonth(), [pastDate]));
    fixture.detectChanges();

    expect(component.selectedDay()?.completed).toBe(true);
    expect(component.success()).toContain(`completed for ${component.formatDate(pastDate)}`);
  });

  it('retains the saved completion and reports an error when an undo request fails', () => {
    flushInitial([habit], { [habit.id]: [logForToday(true)] });

    component.completeToday(habit);
    http
      .expectOne(`${API_BASE_URL}/api/habits/${habit.id}/complete`)
      .flush(
        { message: 'Completion service unavailable' },
        { status: 503, statusText: 'Unavailable' },
      );
    http.expectOne(`${API_BASE_URL}/api/habits`).flush([habit]);
    http.expectOne(`${API_BASE_URL}/api/habits/statistics`).flush({
      totalHabits: 1,
      activeHabits: 1,
      totalCompletedDays: 8,
      completionsThisWeek: 3,
      completionsThisMonth: 6,
      weeklyConsistencyPercentage: 75,
      monthlyConsistencyPercentage: 60,
    });
    http.expectOne(`${API_BASE_URL}/api/habits/${habit.id}/logs`).flush([logForToday(true)]);
    http.expectOne(`${API_BASE_URL}/api/habits/${habit.id}/streak`).flush({
      currentStreak: 3,
      longestStreak: 7,
      totalCompletedDays: 8,
      completionPercentage: 66.67,
      periodUnit: 'days',
    });
    const now = new Date();
    http
      .expectOne((request) => request.url === `${API_BASE_URL}/api/habits/${habit.id}/calendar`)
      .flush(calendarResponse(now.getFullYear(), now.getMonth() + 1));
    fixture.detectChanges();

    expect(component.todayLog(habit.id)?.completed).toBe(true);
    expect(component.actionError()).toBe('Completion service unavailable');
  });

  it('loads the selected month calendar and shows backend completion details', () => {
    flushInitial([habit]);
    component.changeMonth(-1);
    const previousMonth = new Date(new Date().getFullYear(), new Date().getMonth() - 1, 1);
    const request = http.expectOne(
      (candidate) => candidate.url === `${API_BASE_URL}/api/habits/${habit.id}/calendar`,
    );
    expect(request.request.params.get('year')).toBe(String(previousMonth.getFullYear()));
    expect(request.request.params.get('month')).toBe(String(previousMonth.getMonth() + 1));
    request.flush(calendarResponse(previousMonth.getFullYear(), previousMonth.getMonth() + 1));
    fixture.detectChanges();

    const completedDay = component.calendar()?.days[0];
    expect(completedDay?.completed).toBe(true);
    component.selectedCalendarDate.set(completedDay?.date ?? null);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('First day');
    expect(fixture.nativeElement.textContent).toContain('Completed');
  });

  it('confirms deletion before removing a habit and then refreshes habit data', async () => {
    flushInitial([habit]);
    const confirm = vi
      .spyOn(TestBed.inject(ConfirmationDialogService), 'confirm')
      .mockResolvedValue(true);

    await component.deleteHabit(habit);

    expect(confirm).toHaveBeenCalled();
    const deletion = http.expectOne(`${API_BASE_URL}/api/habits/${habit.id}`);
    expect(deletion.request.method).toBe('DELETE');
    deletion.flush(null);

    http.expectOne(`${API_BASE_URL}/api/habits`).flush([]);
    http.expectOne(`${API_BASE_URL}/api/habits/statistics`).flush({
      totalHabits: 0,
      activeHabits: 0,
      totalCompletedDays: 0,
      completionsThisWeek: 0,
      completionsThisMonth: 0,
      weeklyConsistencyPercentage: 0,
      monthlyConsistencyPercentage: 0,
    });
    fixture.detectChanges();

    expect(component.habitDetails()).toEqual([]);
    expect(component.success()).toBe('Habit and its history deleted.');
    confirm.mockRestore();
  });

  it('surfaces habit API errors with a retry action', () => {
    fixture.detectChanges();
    http.expectOne(`${API_BASE_URL}/api/habits/statistics`).flush({
      totalHabits: 0,
      activeHabits: 0,
      totalCompletedDays: 0,
      completionsThisWeek: 0,
      completionsThisMonth: 0,
      weeklyConsistencyPercentage: 0,
      monthlyConsistencyPercentage: 0,
    });
    http
      .expectOne(`${API_BASE_URL}/api/habits`)
      .flush({ message: 'Habit service unavailable' }, { status: 503, statusText: 'Unavailable' });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Habit service unavailable');
    expect(fixture.nativeElement.textContent).toContain('Retry');
  });
});

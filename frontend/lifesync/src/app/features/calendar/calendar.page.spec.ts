import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../core/api.config';
import { ConfirmationDialogService } from '../../shared/confirmation-dialog/confirmation-dialog.service';
import {
  CalendarEventResponse,
  CalendarHabitOccurrence,
  CalendarMilestone,
  CalendarReminder,
  CalendarTask,
  PageResponse,
  ScheduleSlotResponse,
} from './data/calendar.models';
import { CalendarPage } from './calendar.page';

const eventFor = (date: string): CalendarEventResponse => ({
  id: 8,
  title: 'Planning meeting',
  description: 'Review the week',
  eventType: 'MEETING',
  allDay: false,
  startDateTime: `${date}T09:00:00`,
  endDateTime: `${date}T10:00:00`,
  recurrenceType: 'NONE',
  taskId: null,
  taskTitle: null,
  createdAt: `${date}T08:00:00`,
  updatedAt: `${date}T08:00:00`,
});

const scheduleFor = (date: string): ScheduleSlotResponse => ({
  id: 13,
  title: 'Focus block',
  description: 'Deep work',
  startDateTime: `${date}T10:00:00`,
  endDateTime: `${date}T11:00:00`,
  taskId: null,
  taskTitle: null,
  createdAt: `${date}T08:00:00`,
  updatedAt: `${date}T08:00:00`,
});

const taskFor = (date: string): CalendarTask => ({
  id: 21,
  title: 'Submit report',
  status: 'TODO',
  dueDate: `${date}T17:00:00`,
  startDate: null,
});

function taskResponse(date: string) {
  return {
    ...taskFor(date),
    description: null,
    categoryId: null,
    categoryName: null,
    goalId: null,
    milestoneId: null,
    priority: 'MEDIUM' as const,
    estimatedDuration: null,
    actualDuration: null,
    completedAt: null,
    recurring: false,
    recurrenceType: 'NONE' as const,
    createdAt: `${date}T08:00:00`,
    updatedAt: `${date}T08:00:00`,
  };
}

function schedulePage(content: ScheduleSlotResponse[]): PageResponse<ScheduleSlotResponse> {
  return {
    content,
    page: 0,
    size: 100,
    totalElements: content.length,
    totalPages: content.length ? 1 : 0,
    last: true,
  };
}

interface RelatedCalendarRecords {
  goals?: Array<{ id: number }>;
  milestones?: CalendarMilestone[];
  habits?: Array<{ id: number; name: string }>;
  habitOccurrences?: CalendarHabitOccurrence[];
  reminders?: CalendarReminder[];
}

describe('CalendarPage', () => {
  let fixture: ComponentFixture<CalendarPage>;
  let component: CalendarPage;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    fixture = TestBed.createComponent(CalendarPage);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify({ ignoreCancelled: true }));

  function flushMonth(
    events: CalendarEventResponse[] = [],
    schedules: ScheduleSlotResponse[] = [],
    tasks: ReturnType<typeof taskResponse>[] = [],
    related: RelatedCalendarRecords = {},
  ): void {
    fixture.detectChanges();
    const now = new Date();
    const monthRequest = http.expectOne(
      (request) => request.url === `${API_BASE_URL}/api/calendar/events/month`,
    );
    expect(monthRequest.request.params.get('year')).toBe(String(now.getFullYear()));
    expect(monthRequest.request.params.get('month')).toBe(String(now.getMonth() + 1));
    monthRequest.flush(events);
    http
      .expectOne((request) => request.url === `${API_BASE_URL}/api/schedule`)
      .flush(schedulePage(schedules));
    const taskRequest = http.expectOne(
      (request) =>
        request.url === `${API_BASE_URL}/api/tasks` &&
        request.params.get('page') === '0' &&
        request.params.get('size') === '100',
    );
    expect(taskRequest.request.params.get('dueFrom')).toMatch(/^\d{4}-\d{2}-\d{2}T00:00:00/);
    expect(taskRequest.request.params.get('dueTo')).toMatch(/^\d{4}-\d{2}-\d{2}T23:59:59/);
    taskRequest.flush({
      content: tasks,
      page: 0,
      size: 100,
      totalElements: tasks.length,
      totalPages: tasks.length ? 1 : 0,
      last: true,
    });
    http
      .expectOne((request) =>
        request.url === `${API_BASE_URL}/api/reminders` &&
        request.params.get('page') === '0' &&
        request.params.get('size') === '100',
      )
      .flush({
        content: related.reminders ?? [],
        page: 0,
        size: 100,
        totalElements: related.reminders?.length ?? 0,
        totalPages: related.reminders?.length ? 1 : 0,
        last: true,
      });
    http.expectOne(`${API_BASE_URL}/api/goals`).flush(related.goals ?? []);
    http.expectOne(`${API_BASE_URL}/api/habits`).flush(
      (related.habits ?? []).map((habit) => ({
        ...habit,
        active: true,
      })),
    );
    for (const goal of related.goals ?? []) {
      http.expectOne(`${API_BASE_URL}/api/goals/${goal.id}/milestones`).flush(
        (related.milestones ?? []).filter((item) => item.goalId === goal.id),
      );
    }
    const calendarDate = new Date();
    const year = calendarDate.getFullYear();
    const month = calendarDate.getMonth() + 1;
    for (const habit of related.habits ?? []) {
      const request = http.expectOne(
        (item) =>
          item.url === `${API_BASE_URL}/api/habits/${habit.id}/calendar` &&
          item.params.get('year') === String(year) &&
          item.params.get('month') === String(month),
      );
      const days = (related.habitOccurrences ?? [])
        .filter((occurrence) => occurrence.habitId === habit.id)
        .map((occurrence) => ({
          date: occurrence.date,
          eligible: true,
          completed: occurrence.completed,
          notes: null,
        }));
      request.flush({
        habitId: habit.id,
        year,
        month,
        completedDays: days.filter((day) => day.completed).length,
        eligibleDays: days.length,
        completionPercentage: 100,
        days,
      });
    }
    fixture.detectChanges();
  }

  it('renders actual event, schedule, and due-task records and filters them', () => {
    const date = component.selectedDate();
    flushMonth([eventFor(date)], [scheduleFor(date)], [taskResponse(date)]);

    expect(fixture.nativeElement.textContent).toContain('Planning meeting');
    expect(fixture.nativeElement.textContent).toContain('Focus block');
    expect(fixture.nativeElement.textContent).toContain('Submit report');
    expect(component.selectedEntries().map((entry) => entry.source)).toEqual([
      'EVENT',
      'SCHEDULE',
      'TASK',
    ]);

    component.setEventTypeFilter('WORK');
    expect(component.visibleEvents()).toEqual([]);
    component.setEventTypeFilter('MEETING');
    expect(component.visibleEvents()).toHaveLength(1);
    component.showSchedules.set(false);
    expect(component.selectedEntries().some((entry) => entry.source === 'SCHEDULE')).toBe(false);
    component.searchText.set('report');
    expect(component.selectedEntries().map((entry) => entry.title)).toEqual(['Submit report']);
  });

  it('shows reminders, milestones, and eligible habit occurrences on their dates', () => {
    const date = component.selectedDate();
    const reminder: CalendarReminder = {
      id: 25,
      title: 'Water the plants',
      message: 'Garden reminder',
      reminderDateTime: `${date}T09:00:00`,
      status: 'PENDING',
      reminderType: 'GENERAL',
    };
    const milestone: CalendarMilestone = {
      id: 32,
      goalId: 7,
      title: 'Finish first draft',
      description: 'Complete milestone',
      targetDate: date,
      status: 'IN_PROGRESS',
    };
    const habit: CalendarHabitOccurrence = {
      habitId: 12,
      habitName: 'Morning walk',
      date,
      completed: true,
    };
    flushMonth([], [], [], {
      goals: [{ id: 7 }],
      milestones: [milestone],
      habits: [{ id: 12, name: habit.habitName }],
      habitOccurrences: [habit],
      reminders: [reminder],
    });

    expect(component.selectedEntries().map((entry) => entry.source)).toEqual([
      'MILESTONE',
      'HABIT',
      'REMINDER',
    ]);
    expect(fixture.nativeElement.textContent).toContain('Water the plants');
    expect(fixture.nativeElement.textContent).toContain('Finish first draft');
    expect(fixture.nativeElement.textContent).toContain('Morning walk');
  });

  it('searches linked task titles on events and schedule entries', () => {
    const date = component.selectedDate();
    const linkedEvent = { ...eventFor(date), taskId: 21, taskTitle: 'Learn Coding' };
    const linkedSchedule = { ...scheduleFor(date), taskId: 22, taskTitle: 'Study Angular' };
    const linkedTask = { ...taskResponse(date), title: 'Coding practice' };
    flushMonth([linkedEvent], [linkedSchedule], [linkedTask]);

    component.searchText.set('  COD  ');
    expect(component.selectedEntries().map((entry) => entry.source)).toEqual(['EVENT', 'TASK']);
    component.searchText.set(' ANG ');
    expect(component.selectedEntries().map((entry) => entry.source)).toEqual(['SCHEDULE']);
    component.searchText.set('no-such-calendar-item');
    expect(component.selectedEntries()).toEqual([]);
    component.searchText.set('');
    expect(component.selectedEntries()).toHaveLength(3);
  });

  it('shows loading and empty states, then reports API failures with retry', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Loading…');
    const month = http.expectOne(
      (request) => request.url === `${API_BASE_URL}/api/calendar/events/month`,
    );
    month.flush([]);
    http
      .expectOne((request) => request.url === `${API_BASE_URL}/api/schedule`)
      .flush(schedulePage([]));
    http
      .expectOne((request) => request.url === `${API_BASE_URL}/api/tasks`)
      .flush({
        content: [],
        page: 0,
        size: 100,
        totalElements: 0,
        totalPages: 0,
        last: true,
      });
    flushRelated([]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No items for this date');

    component.loadCalendar();
    const failedMonth = http.expectOne(
      (request) => request.url === `${API_BASE_URL}/api/calendar/events/month`,
    );
    failedMonth.flush(
      { message: 'Calendar unavailable' },
      { status: 503, statusText: 'Unavailable' },
    );
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Calendar unavailable');
    expect(fixture.nativeElement.textContent).toContain('Retry');
  });

  it('navigates periods and switches between month, week, and day backend queries', () => {
    flushMonth();
    const initialMonth = component.currentDate().getMonth();
    component.navigate(1);
    expect(component.currentDate().getMonth()).toBe((initialMonth + 1) % 12);
    const nextMonth = http.expectOne(
      (request) => request.url === `${API_BASE_URL}/api/calendar/events/month`,
    );
    nextMonth.flush([]);
    http
      .expectOne((request) => request.url === `${API_BASE_URL}/api/schedule`)
      .flush(schedulePage([]));
    http
      .expectOne((request) => request.url === `${API_BASE_URL}/api/tasks`)
      .flush({
        content: [],
        page: 0,
        size: 100,
        totalElements: 0,
        totalPages: 0,
        last: true,
      });
    flushRelated([]);

    component.setView('WEEK');
    const weekStart = component.weekDates()[0];
    const weekEventRequest = http.expectOne(
      (request) => request.url === `${API_BASE_URL}/api/calendar/events/week`,
    );
    expect(weekEventRequest.request.params.get('startDate')).toBe(weekStart);
    weekEventRequest.flush([]);
    const weekScheduleRequest = http.expectOne(
      (request) => request.url === `${API_BASE_URL}/api/schedule/weekly`,
    );
    expect(weekScheduleRequest.request.params.get('startDate')).toBe(weekStart);
    weekScheduleRequest.flush([]);
    http
      .expectOne((request) => request.url === `${API_BASE_URL}/api/tasks`)
      .flush({
        content: [],
        page: 0,
        size: 100,
        totalElements: 0,
        totalPages: 0,
        last: true,
      });
    flushRelated([]);

    component.setView('DAY');
    http
      .expectOne((request) => request.url === `${API_BASE_URL}/api/calendar/events/day`)
      .flush([]);
    http.expectOne((request) => request.url === `${API_BASE_URL}/api/schedule/daily`).flush([]);
    http
      .expectOne((request) => request.url === `${API_BASE_URL}/api/tasks`)
      .flush({
        content: [],
        page: 0,
        size: 100,
        totalElements: 0,
        totalPages: 0,
        last: true,
      });
    flushRelated([]);
    expect(component.view()).toBe('DAY');
  });

  function flushRelated(goals: Array<{ id: number }>): void {
    http.expectOne((request) => request.url === `${API_BASE_URL}/api/reminders`).flush({
      content: [], page: 0, size: 100, totalElements: 0, totalPages: 0, last: true,
    });
    http.expectOne(`${API_BASE_URL}/api/goals`).flush(goals);
    http.expectOne(`${API_BASE_URL}/api/habits`).flush([]);
    for (const goal of goals) {
      http.expectOne(`${API_BASE_URL}/api/goals/${goal.id}/milestones`).flush([]);
    }
  }

  it('creates an event and refreshes the calendar after backend confirmation', () => {
    flushMonth();
    component.saveEvent({
      id: null,
      request: {
        title: 'Created event',
        description: null,
        eventType: 'PERSONAL',
        allDay: false,
        startDateTime: '2026-11-12T09:00:00',
        endDateTime: '2026-11-12T10:00:00',
        recurrenceType: 'NONE',
        taskId: null,
      },
    });
    const create = http.expectOne(`${API_BASE_URL}/api/calendar/events`);
    expect(create.request.method).toBe('POST');
    create.flush(eventFor('2026-11-12'));
    expect(component.success()).toBe('Calendar event created.');

    http
      .expectOne((request) => request.url === `${API_BASE_URL}/api/calendar/events/month`)
      .flush([]);
    http
      .expectOne((request) => request.url === `${API_BASE_URL}/api/schedule`)
      .flush(schedulePage([]));
    http
      .expectOne((request) => request.url === `${API_BASE_URL}/api/tasks`)
      .flush({
        content: [],
        page: 0,
        size: 100,
        totalElements: 0,
        totalPages: 0,
        last: true,
      });
    flushRelated([]);
  });

  it('shows schedule conflicts from the backend and does not create overlapping entries', () => {
    flushMonth();
    component.saveSchedule({
      id: null,
      request: {
        title: 'Focus',
        description: null,
        startDateTime: '2026-11-12T09:00:00',
        endDateTime: '2026-11-12T10:00:00',
        taskId: null,
      },
    });
    const conflictRequest = http.expectOne(`${API_BASE_URL}/api/schedule/conflicts`);
    expect(conflictRequest.request.method).toBe('POST');
    conflictRequest.flush({
      hasConflicts: true,
      conflicts: [
        {
          type: 'CALENDAR_EVENT',
          id: 8,
          title: 'Meeting',
          startDateTime: '2026-11-12T09:30:00',
          endDateTime: '2026-11-12T10:30:00',
        },
      ],
    });
    expect(component.conflicts()[0].title).toBe('Meeting');
    expect(component.scheduleFormError()).toContain('overlap');
    http.expectNone(`${API_BASE_URL}/api/schedule`);
  });

  it('deletes a repeating event series only after confirmation', async () => {
    const date = component.selectedDate();
    const recurring = { ...eventFor(date), recurrenceType: 'WEEKLY' as const };
    flushMonth([recurring]);
    const confirm = vi
      .spyOn(TestBed.inject(ConfirmationDialogService), 'confirm')
      .mockResolvedValue(true);
    await component.deleteEvent(recurring);
    expect(confirm).toHaveBeenCalledWith({
      title: 'Delete this event?',
      message: 'Delete "Planning meeting" and its entire repeating series?',
      confirmLabel: 'Yes, Delete',
    });
    const deletion = http.expectOne(`${API_BASE_URL}/api/calendar/events/8`);
    expect(deletion.request.method).toBe('DELETE');
    deletion.flush(null);
    http
      .expectOne((request) => request.url === `${API_BASE_URL}/api/calendar/events/month`)
      .flush([]);
    http
      .expectOne((request) => request.url === `${API_BASE_URL}/api/schedule`)
      .flush(schedulePage([]));
    http
      .expectOne((request) => request.url === `${API_BASE_URL}/api/tasks`)
      .flush({
        content: [],
        page: 0,
        size: 100,
        totalElements: 0,
        totalPages: 0,
        last: true,
      });
    flushRelated([]);
    expect(component.success()).toBe('Calendar event deleted.');
    confirm.mockRestore();
  });
});

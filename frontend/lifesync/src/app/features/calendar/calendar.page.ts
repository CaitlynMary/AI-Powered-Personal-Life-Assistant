import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize, forkJoin, map, of, switchMap } from 'rxjs';
import { readApiError } from '../../core/auth/auth-error';
import { RegionalDateService } from '../../core/services/regional-date.service';
import { ConfirmationDialogService } from '../../shared/confirmation-dialog/confirmation-dialog.service';
import { TaskApiService } from '../tasks/data/task-api.service';
import { GoalApiService } from '../goals/data/goal-api.service';
import { MilestoneApiService } from '../goals/data/milestone-api.service';
import { HabitApiService } from '../habits/data/habit-api.service';
import { HabitLogApiService } from '../habits/data/habit-log-api.service';
import { ReminderApiService } from '../reminders/data/reminder-api.service';
import {
  CalendarHabitOccurrence,
  CalendarMilestone,
  CalendarReminder,
  CalendarData,
  CalendarEventRequest,
  CalendarEventResponse,
  CalendarEventType,
  CalendarTask,
  ScheduleConflictItem,
  ScheduleConflictRequest,
  ScheduleSlotRequest,
  ScheduleSlotResponse,
} from './data/calendar.models';
import { CalendarEventApiService } from './data/calendar-event-api.service';
import { ScheduleApiService } from './data/schedule-api.service';
import { CalendarEventForm } from './event-form/event-form';
import { ScheduleForm } from './schedule-form/schedule-form';

type CalendarView = 'MONTH' | 'WEEK' | 'DAY';
type CalendarEntrySource = 'EVENT' | 'SCHEDULE' | 'TASK' | 'REMINDER' | 'MILESTONE' | 'HABIT';

interface PlannerEntry {
  source: CalendarEntrySource;
  id: number;
  title: string;
  description: string | null;
  start: string | null;
  end: string | null;
  eventType: CalendarEventType | null;
  status: string | null;
  allDay: boolean;
  linkedTaskTitle: string | null;
  recurrenceType: string | null;
}

@Component({
  selector: 'app-calendar-page',
  standalone: true,
  imports: [FormsModule, RouterLink, CalendarEventForm, ScheduleForm],
  templateUrl: './calendar.page.html',
})
export class CalendarPage {
  private readonly eventApi = inject(CalendarEventApiService);
  private readonly scheduleApi = inject(ScheduleApiService);
  private readonly taskApi = inject(TaskApiService);
  private readonly goalApi = inject(GoalApiService);
  private readonly milestoneApi = inject(MilestoneApiService);
  private readonly habitApi = inject(HabitApiService);
  private readonly habitLogApi = inject(HabitLogApiService);
  private readonly reminderApi = inject(ReminderApiService);
  private readonly confirmation = inject(ConfirmationDialogService);
  private readonly regionalDate = inject(RegionalDateService);
  private loadRequestId = 0;

  readonly currentDate = signal(this.startOfDay(new Date()));
  readonly selectedDate = signal(this.toLocalDate(new Date()));
  readonly view = signal<CalendarView>('MONTH');
  readonly data = signal<CalendarData>({
    events: [], schedules: [], tasks: [], reminders: [], milestones: [], habits: [],
  });
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly eventFormOpen = signal(false);
  readonly scheduleFormOpen = signal(false);
  readonly editingEvent = signal<CalendarEventResponse | null>(null);
  readonly editingSchedule = signal<ScheduleSlotResponse | null>(null);
  readonly eventFormError = signal<string | null>(null);
  readonly scheduleFormError = signal<string | null>(null);
  readonly conflicts = signal<ScheduleConflictItem[]>([]);
  readonly savingEvent = signal(false);
  readonly savingSchedule = signal(false);
  readonly deletingEventId = signal<number | null>(null);
  readonly deletingScheduleId = signal<number | null>(null);
  readonly eventTypeFilter = signal<CalendarEventType | ''>('');
  readonly searchText = signal('');
  readonly showSchedules = signal(true);
  readonly showTasks = signal(true);
  readonly dayNames = computed(() => {
    const start = this.regionalDate.weekStartIndex();
    return Array.from({ length: 7 }, (_, index) =>
      this.regionalDate
        .formatDateOnly(new Date(2023, 0, 1 + ((start + index) % 7)), { weekday: 'short' })
        .slice(0, 3),
    );
  });
  readonly monthCells = computed(() => {
    const date = this.currentDate();
    const first = new Date(date.getFullYear(), date.getMonth(), 1);
    const count = new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate();
    const offset = (first.getDay() - this.regionalDate.weekStartIndex() + 7) % 7;
    return [
      ...Array<string | null>(offset).fill(null),
      ...Array.from({ length: count }, (_, index) =>
        this.toLocalDate(new Date(date.getFullYear(), date.getMonth(), index + 1)),
      ),
    ];
  });
  readonly weekDates = computed(() => {
    const selected = this.parseLocalDate(this.selectedDate());
    const weekday = selected.getDay();
    const weekStartOffset = (weekday - this.regionalDate.weekStartIndex() + 7) % 7;
    return Array.from({ length: 7 }, (_, index) => {
      const date = new Date(selected);
      date.setDate(selected.getDate() - weekStartOffset + index);
      return this.toLocalDate(date);
    });
  });
  readonly selectedEntries = computed(() => this.entriesForDate(this.selectedDate()));
  readonly visibleEvents = computed(() => {
    const search = this.searchText().trim().toLowerCase();
    return this.data().events.filter(
      (event) =>
        (!this.eventTypeFilter() || event.eventType === this.eventTypeFilter()) &&
        (!search ||
          event.title.toLowerCase().includes(search) ||
          (event.description ?? '').toLowerCase().includes(search) ||
          (event.taskTitle ?? '').toLowerCase().includes(search)),
    );
  });

  constructor() {
    this.loadCalendar();
  }

  loadCalendar(): void {
    const requestId = ++this.loadRequestId;
    this.loading.set(true);
    this.error.set(null);
    const range = this.visibleRange();
    const events =
      this.view() === 'DAY'
        ? this.eventApi.day(this.selectedDate())
        : this.view() === 'WEEK'
          ? this.eventApi.week(this.weekDates()[0])
          : this.eventApi.month(range.year, range.month);
    const schedules =
      this.view() === 'DAY'
        ? this.scheduleApi.daily(this.selectedDate())
        : this.view() === 'WEEK'
          ? this.scheduleApi.weekly(this.weekDates()[0])
          : this.scheduleApi
              .listAll()
              .pipe(
                map((items) =>
                  items.filter((item) =>
                    this.overlapsRange(
                      item.startDateTime,
                      item.endDateTime,
                      range.start,
                      range.end,
                    ),
                  ),
                ),
              );
    forkJoin({
      events,
      schedules,
      reminders: this.reminderApi.listAll().pipe(
        map((items) => items.filter((item) =>
          item.status !== 'CANCELLED' &&
          item.reminderDateTime.slice(0, 10) >= range.start &&
          item.reminderDateTime.slice(0, 10) <= range.end,
        )),
      ),
      milestones: this.goalApi.list().pipe(
        switchMap((goals) => goals.length
          ? forkJoin(goals.map((goal) => this.milestoneApi.listForGoal(goal.id))).pipe(
              map((milestones) => milestones.flat().filter((item) =>
                item.targetDate >= range.start && item.targetDate <= range.end,
              )),
            )
          : of([] as CalendarMilestone[]),
        ),
      ),
      habits: this.habitApi.list().pipe(
        switchMap((habits) => {
          const months = this.monthsInRange(range.start, range.end);
          const requests = habits
            .filter((habit) => habit.active)
            .flatMap((habit) => months.map(({ year, month }) =>
              this.habitLogApi.calendar(habit.id, year, month).pipe(
                map((calendar) => calendar.days
                  .filter((day) => day.eligible && day.date >= range.start && day.date <= range.end)
                  .map((day): CalendarHabitOccurrence => ({
                    habitId: habit.id,
                    habitName: habit.name,
                    date: day.date,
                    completed: day.completed,
                  })),
                ),
              ),
            ));
          return requests.length
            ? forkJoin(requests).pipe(map((items) => items.flat()))
            : of([] as CalendarHabitOccurrence[]);
        }),
      ),
      tasks: this.taskApi.listAll({
        dueFrom: `${range.start}T00:00:00`,
        dueTo: `${range.end}T23:59:59.999999999`,
      }),
    })
      .pipe(
        map(({ events, schedules, tasks, reminders, milestones, habits }): CalendarData => ({
          events,
          schedules,
          reminders,
          milestones,
          habits,
          tasks: tasks.map((task) => ({
            id: task.id,
            title: task.title,
            status: task.status,
            dueDate: task.dueDate,
            startDate: task.startDate,
          })),
        })),
        finalize(() => {
          if (requestId === this.loadRequestId) this.loading.set(false);
        }),
      )
      .subscribe({
        next: (data) => {
          if (requestId === this.loadRequestId) this.data.set(data);
        },
        error: (error: unknown) => {
          if (requestId === this.loadRequestId) {
            this.error.set(readApiError(error, 'Could not load calendar data. Please try again.'));
          }
        },
      });
  }

  setView(view: CalendarView): void {
    if (view === this.view()) return;
    this.view.set(view);
    this.loadCalendar();
  }

  goToday(): void {
    const today = this.startOfDay(new Date());
    this.currentDate.set(today);
    this.selectedDate.set(this.toLocalDate(today));
    this.loadCalendar();
  }

  navigate(offset: number): void {
    const selected = this.parseLocalDate(this.selectedDate());
    if (this.view() === 'MONTH') {
      const next = new Date(selected.getFullYear(), selected.getMonth() + offset, 1);
      const date = this.toLocalDate(next);
      this.selectedDate.set(date);
      this.currentDate.set(next);
    } else {
      const next = new Date(selected);
      next.setDate(next.getDate() + offset * (this.view() === 'WEEK' ? 7 : 1));
      this.selectedDate.set(this.toLocalDate(next));
      this.currentDate.set(next);
    }
    this.loadCalendar();
  }

  selectDate(date: string): void {
    this.selectedDate.set(date);
    this.currentDate.set(this.parseLocalDate(date));
    if (this.view() === 'DAY') this.loadCalendar();
  }

  setEventTypeFilter(value: string): void {
    if (['', 'PERSONAL', 'WORK', 'MEETING', 'TASK', 'OTHER'].includes(value)) {
      this.eventTypeFilter.set(value as CalendarEventType | '');
    }
  }

  beginCreateEvent(): void {
    this.editingEvent.set(null);
    this.eventFormError.set(null);
    this.eventFormOpen.set(true);
  }

  editEvent(occurrence: CalendarEventResponse): void {
    this.actionError.set(null);
    this.eventApi.get(occurrence.id).subscribe({
      next: (event) => {
        this.editingEvent.set(event);
        this.eventFormError.set(null);
        this.eventFormOpen.set(true);
      },
      error: (error: unknown) => {
        this.actionError.set(readApiError(error, 'Could not load this event for editing.'));
      },
    });
  }

  saveEvent(event: { id: number | null; request: CalendarEventRequest }): void {
    this.savingEvent.set(true);
    this.eventFormError.set(null);
    const operation =
      event.id === null
        ? this.eventApi.create(event.request)
        : this.eventApi.update(event.id, event.request);
    operation.pipe(finalize(() => this.savingEvent.set(false))).subscribe({
      next: () => {
        this.eventFormOpen.set(false);
        this.success.set(event.id === null ? 'Calendar event created.' : 'Calendar event updated.');
        this.loadCalendar();
      },
      error: (error: unknown) => {
        this.eventFormError.set(
          readApiError(error, 'Could not save this event. Check its time range and conflicts.'),
        );
      },
    });
  }

  async deleteEvent(event: CalendarEventResponse): Promise<void> {
    const message =
      event.recurrenceType === 'NONE'
        ? `Delete "${event.title}"?`
        : `Delete "${event.title}" and its entire repeating series?`;
    if (
      !(await this.confirmation.confirm({
        title: 'Delete this event?',
        message,
        confirmLabel: 'Yes, Delete',
      }))
    ) return;
    this.deletingEventId.set(event.id);
    this.actionError.set(null);
    this.eventApi
      .delete(event.id)
      .pipe(finalize(() => this.deletingEventId.set(null)))
      .subscribe({
        next: () => {
          this.success.set('Calendar event deleted.');
          this.loadCalendar();
        },
        error: (error: unknown) => {
          this.actionError.set(readApiError(error, 'Could not delete this calendar event.'));
        },
      });
  }

  beginCreateSchedule(): void {
    this.editingSchedule.set(null);
    this.scheduleFormError.set(null);
    this.conflicts.set([]);
    this.scheduleFormOpen.set(true);
  }

  editSchedule(slot: ScheduleSlotResponse): void {
    this.editingSchedule.set(slot);
    this.scheduleFormError.set(null);
    this.conflicts.set([]);
    this.scheduleFormOpen.set(true);
  }

  saveSchedule(event: { id: number | null; request: ScheduleSlotRequest }): void {
    this.savingSchedule.set(true);
    this.scheduleFormError.set(null);
    this.conflicts.set([]);
    const save = () => {
      const operation =
        event.id === null
          ? this.scheduleApi.create(event.request)
          : this.scheduleApi.update(event.id, event.request);
      operation.pipe(finalize(() => this.savingSchedule.set(false))).subscribe({
        next: () => {
          this.scheduleFormOpen.set(false);
          this.success.set(
            event.id === null ? 'Schedule entry created.' : 'Schedule entry updated.',
          );
          this.loadCalendar();
        },
        error: (error: unknown) => {
          this.scheduleFormError.set(
            readApiError(error, 'Could not save this schedule entry. It may overlap another item.'),
          );
        },
      });
    };

    if (event.id !== null) {
      save();
      return;
    }

    const conflictRequest: ScheduleConflictRequest = {
      startDateTime: event.request.startDateTime,
      endDateTime: event.request.endDateTime,
      taskId: event.request.taskId,
    };
    this.scheduleApi.detectConflicts(conflictRequest).subscribe({
      next: (result) => {
        if (result.hasConflicts) {
          this.conflicts.set(result.conflicts);
          this.scheduleFormError.set(
            'Please choose a time that does not overlap existing calendar items.',
          );
          this.savingSchedule.set(false);
          return;
        }
        save();
      },
      error: (error: unknown) => {
        this.savingSchedule.set(false);
        this.scheduleFormError.set(
          readApiError(error, 'Could not check for schedule conflicts. Nothing was saved.'),
        );
      },
    });
  }

  async deleteSchedule(slot: ScheduleSlotResponse): Promise<void> {
    if (
      !(await this.confirmation.confirm({
        title: 'Delete this schedule entry?',
        message: `Delete schedule entry "${slot.title}"?`,
        confirmLabel: 'Yes, Delete',
      }))
    ) return;
    this.deletingScheduleId.set(slot.id);
    this.actionError.set(null);
    this.scheduleApi
      .delete(slot.id)
      .pipe(finalize(() => this.deletingScheduleId.set(null)))
      .subscribe({
        next: () => {
          this.success.set('Schedule entry deleted.');
          this.loadCalendar();
        },
        error: (error: unknown) => {
          this.actionError.set(readApiError(error, 'Could not delete this schedule entry.'));
        },
      });
  }

  entriesForDate(date: string): PlannerEntry[] {
    const search = this.searchText().trim().toLowerCase();
    const entries: PlannerEntry[] = [];
    if (this.view() !== 'MONTH' || date.startsWith(this.monthPrefix())) {
      for (const event of this.visibleEvents()) {
        if (this.overlapsDate(event.startDateTime, event.endDateTime, date)) {
          entries.push({
            source: 'EVENT',
            id: event.id,
            title: event.title,
            description: event.description,
            start: event.startDateTime,
            end: event.endDateTime,
            eventType: event.eventType,
            status: null,
            allDay: event.allDay,
            linkedTaskTitle: event.taskTitle,
            recurrenceType: event.recurrenceType,
          });
        }
      }
    }
    if (this.showSchedules()) {
      for (const slot of this.data().schedules) {
        if (
          this.overlapsDate(slot.startDateTime, slot.endDateTime, date) &&
          (!search ||
            slot.title.toLowerCase().includes(search) ||
            (slot.description ?? '').toLowerCase().includes(search) ||
            (slot.taskTitle ?? '').toLowerCase().includes(search))
        ) {
          entries.push({
            source: 'SCHEDULE',
            id: slot.id,
            title: slot.title,
            description: slot.description,
            start: slot.startDateTime,
            end: slot.endDateTime,
            eventType: null,
            status: null,
            allDay: false,
            linkedTaskTitle: slot.taskTitle,
            recurrenceType: null,
          });
        }
      }
    }
    if (this.showTasks()) {
      for (const task of this.data().tasks) {
        if (
          task.dueDate?.startsWith(date) &&
          (!search || task.title.toLowerCase().includes(search))
        ) {
          entries.push({
            source: 'TASK',
            id: task.id,
            title: task.title,
            description: null,
            start: task.dueDate,
            end: null,
            eventType: null,
            status: task.status,
            allDay: false,
            linkedTaskTitle: null,
            recurrenceType: null,
          });
        }
      }
    }
    for (const reminder of this.data().reminders) {
      if (
        reminder.reminderDateTime.startsWith(date) &&
        (!search ||
          reminder.title.toLowerCase().includes(search) ||
          reminder.message.toLowerCase().includes(search))
      ) {
        entries.push({
          source: 'REMINDER',
          id: reminder.id,
          title: reminder.title,
          description: reminder.message,
          start: reminder.reminderDateTime,
          end: null,
          eventType: null,
          status: reminder.status,
          allDay: false,
          linkedTaskTitle: null,
          recurrenceType: null,
        });
      }
    }
    for (const milestone of this.data().milestones) {
      if (
        milestone.targetDate === date &&
        (!search ||
          milestone.title.toLowerCase().includes(search) ||
          (milestone.description ?? '').toLowerCase().includes(search))
      ) {
        entries.push({
          source: 'MILESTONE',
          id: milestone.id,
          title: milestone.title,
          description: milestone.description,
          start: `${date}T00:00:00`,
          end: null,
          eventType: null,
          status: milestone.status,
          allDay: true,
          linkedTaskTitle: null,
          recurrenceType: null,
        });
      }
    }
    for (const habit of this.data().habits) {
      if (habit.date === date && (!search || habit.habitName.toLowerCase().includes(search))) {
        entries.push({
          source: 'HABIT',
          id: habit.habitId,
          title: habit.habitName,
          description: null,
          start: `${date}T00:00:00`,
          end: null,
          eventType: null,
          status: habit.completed ? 'COMPLETED' : 'PENDING',
          allDay: true,
          linkedTaskTitle: null,
          recurrenceType: null,
        });
      }
    }
    return entries.sort((a, b) => {
      if (!a.start) return 1;
      if (!b.start) return -1;
      return a.start.localeCompare(b.start);
    });
  }

  countForDate(date: string): number {
    return this.entriesForDate(date).length;
  }

  typeLabel(type: CalendarEventType): string {
    return {
      PERSONAL: 'Personal',
      WORK: 'Work',
      MEETING: 'Meeting',
      TASK: 'Task',
      OTHER: 'Other',
    }[type];
  }

  eventTypeClass(type: CalendarEventType): string {
    return {
      PERSONAL: 'bg-soft-pink text-ls-text',
      WORK: 'bg-purple/15 text-purple',
      MEETING: 'bg-mint/35 text-ls-text',
      TASK: 'bg-aqua text-plum',
      OTHER: 'bg-ls-surface-2 text-ls-text',
    }[type];
  }

  sourceLabel(source: CalendarEntrySource): string {
    return {
      EVENT: 'Calendar event', SCHEDULE: 'Schedule entry', TASK: 'Task due',
      REMINDER: 'Reminder', MILESTONE: 'Milestone', HABIT: 'Habit',
    }[source];
  }

  sourceClass(source: CalendarEntrySource, eventType: CalendarEventType | null): string {
    if (source === 'EVENT' && eventType) return this.eventTypeClass(eventType);
    return {
      EVENT: 'bg-purple/15 text-purple',
      SCHEDULE: 'bg-mint/35 text-ls-text',
      TASK: 'bg-aqua text-plum',
      REMINDER: 'bg-soft-pink text-burgundy',
      MILESTONE: 'bg-purple/15 text-purple',
      HABIT: 'bg-mint/35 text-ls-text',
    }[source];
  }

  taskStatusClass(status: string | null): string {
    return {
      TODO: 'bg-ls-bg text-ls-text-muted',
      IN_PROGRESS: 'bg-powder-blue text-plum',
      COMPLETED: 'bg-aqua text-plum',
      OVERDUE: 'bg-burgundy text-white',
      MISSED: 'bg-tangerine text-plum',
      CANCELLED: 'bg-ls-surface-2 text-ls-text-muted',
    }[status ?? ''] ?? 'bg-ls-bg text-ls-text-muted';
  }

  formatTime(value: string | null): string {
    if (!value) return 'No time';
    return this.regionalDate.formatTime(value, { hour: 'numeric', minute: '2-digit' });
  }

  formatDate(date: string): string {
    return this.regionalDate.formatDateOnly(date, {
      weekday: 'long',
      month: 'long',
      day: 'numeric',
      year: 'numeric',
    });
  }

  weekdayLabel(date: string): string {
    return this.regionalDate.formatDateOnly(date, { weekday: 'short' });
  }

  rangeLabel(): string {
    if (this.view() === 'DAY') return this.formatDate(this.selectedDate());
    if (this.view() === 'MONTH') {
      return this.regionalDate.formatDateOnly(this.currentDate(), { month: 'long', year: 'numeric' });
    }
    return `${this.formatShortDate(this.weekDates()[0])} – ${this.formatShortDate(this.weekDates()[6])}`;
  }

  clearSuccess(): void {
    this.success.set(null);
  }

  isToday(date: string): boolean {
    return date === this.toLocalDate(new Date());
  }

  private visibleRange(): { start: string; end: string; year: number; month: number } {
    if (this.view() === 'DAY') {
      return {
        start: this.selectedDate(),
        end: this.selectedDate(),
        year: this.currentDate().getFullYear(),
        month: this.currentDate().getMonth() + 1,
      };
    }
    if (this.view() === 'WEEK') {
      const dates = this.weekDates();
      return {
        start: dates[0],
        end: dates[6],
        year: this.currentDate().getFullYear(),
        month: this.currentDate().getMonth() + 1,
      };
    }
    const date = this.currentDate();
    return {
      start: this.toLocalDate(new Date(date.getFullYear(), date.getMonth(), 1)),
      end: this.toLocalDate(new Date(date.getFullYear(), date.getMonth() + 1, 0)),
      year: date.getFullYear(),
      month: date.getMonth() + 1,
    };
  }

  private overlapsRange(start: string, end: string, rangeStart: string, rangeEnd: string): boolean {
    return start.slice(0, 10) <= rangeEnd && end.slice(0, 10) >= rangeStart;
  }

  private overlapsDate(start: string, end: string, date: string): boolean {
    return start.slice(0, 10) <= date && end.slice(0, 10) >= date;
  }

  private monthsInRange(start: string, end: string): Array<{ year: number; month: number }> {
    const months: Array<{ year: number; month: number }> = [];
    const cursor = this.parseLocalDate(start);
    cursor.setDate(1);
    const last = this.parseLocalDate(end);
    while (cursor <= last) {
      months.push({ year: cursor.getFullYear(), month: cursor.getMonth() + 1 });
      cursor.setMonth(cursor.getMonth() + 1);
    }
    return months;
  }

  private monthPrefix(): string {
    const date = this.currentDate();
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
  }

  private formatShortDate(value: string): string {
    return this.regionalDate.formatDateOnly(value, { month: 'short', day: 'numeric' });
  }

  private startOfDay(date: Date): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate());
  }

  private toLocalDate(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }

  private parseLocalDate(value: string): Date {
    const [year, month, day] = value.split('-').map(Number);
    return new Date(year, month - 1, day);
  }
}

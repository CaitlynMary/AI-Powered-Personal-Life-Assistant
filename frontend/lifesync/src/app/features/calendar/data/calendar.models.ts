export type CalendarEventType = 'PERSONAL' | 'WORK' | 'MEETING' | 'TASK' | 'OTHER';
export type CalendarRecurrenceType = 'NONE' | 'DAILY' | 'WEEKLY' | 'MONTHLY';
export type ScheduleConflictType = 'SCHEDULE_SLOT' | 'CALENDAR_EVENT' | 'TASK' | string;
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'COMPLETED' | 'OVERDUE' | 'MISSED' | 'CANCELLED';

export interface CalendarEventResponse {
  id: number;
  title: string;
  description: string | null;
  eventType: CalendarEventType;
  allDay: boolean;
  startDateTime: string;
  endDateTime: string;
  recurrenceType: CalendarRecurrenceType;
  taskId: number | null;
  taskTitle: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CalendarEventRequest {
  title: string;
  description: string | null;
  eventType: CalendarEventType;
  allDay: boolean;
  startDateTime: string;
  endDateTime: string;
  recurrenceType: CalendarRecurrenceType;
  taskId: number | null;
}

export interface ScheduleSlotResponse {
  id: number;
  title: string;
  description: string | null;
  startDateTime: string;
  endDateTime: string;
  taskId: number | null;
  taskTitle: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ScheduleSlotRequest {
  title: string;
  description: string | null;
  startDateTime: string;
  endDateTime: string;
  taskId: number | null;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ScheduleConflictItem {
  type: ScheduleConflictType;
  id: number;
  title: string;
  startDateTime: string;
  endDateTime: string;
}

export interface ScheduleConflictResponse {
  hasConflicts: boolean;
  conflicts: ScheduleConflictItem[];
}

export interface ScheduleConflictRequest {
  startDateTime: string;
  endDateTime: string;
  taskId: number | null;
}

export interface DailyScheduleApprovalItem {
  taskId: number;
  startTime: string;
  endTime: string;
}

export interface DailyScheduleApprovalRequest {
  date: string;
  items: DailyScheduleApprovalItem[];
}

export interface CalendarTask {
  id: number;
  title: string;
  status: TaskStatus;
  dueDate: string | null;
  startDate: string | null;
}

export interface CalendarReminder {
  id: number;
  title: string;
  message: string;
  reminderDateTime: string;
  status: string;
  reminderType: string;
}

export interface CalendarMilestone {
  id: number;
  goalId: number;
  title: string;
  description: string | null;
  targetDate: string;
  status: string;
}

export interface CalendarHabitOccurrence {
  habitId: number;
  habitName: string;
  date: string;
  completed: boolean;
}

export interface CalendarData {
  events: CalendarEventResponse[];
  schedules: ScheduleSlotResponse[];
  tasks: CalendarTask[];
  reminders: CalendarReminder[];
  milestones: CalendarMilestone[];
  habits: CalendarHabitOccurrence[];
}

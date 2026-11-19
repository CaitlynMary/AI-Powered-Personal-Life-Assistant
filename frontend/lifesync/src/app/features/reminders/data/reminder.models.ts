export type ReminderStatus = 'PENDING' | 'SENT' | 'CANCELLED';
export type ReminderType = 'GENERAL' | 'TASK' | 'GOAL' | 'HABIT' | 'CALENDAR';

export interface ReminderRequest {
  title: string;
  message: string;
  reminderDateTime: string;
  reminderType: ReminderType;
  taskId?: number | null;
  deadlineOffsetMinutes?: number | null;
}

export interface ReminderResponse extends ReminderRequest {
  id: number;
  status: ReminderStatus;
  taskId?: number | null;
  taskTitle?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ReminderPageResponse {
  content: ReminderResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ReminderSummary {
  total: number;
  pending: number;
  upcoming: number;
  sent: number;
}

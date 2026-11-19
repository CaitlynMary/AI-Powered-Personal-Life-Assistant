export type NotificationType =
  | 'REMINDER'
  | 'TASK_DEADLINE_APPROACHING'
  | 'TASK_OVERDUE'
  | 'TASK_COMPLETED'
  | 'TASK_MISSED'
  | 'TASK_RESCHEDULED'
  | 'MILESTONE_COMPLETED'
  | 'HABIT_COMPLETED'
  | 'AI_DAILY_PLAN_READY'
  | 'SYSTEM';

export interface NotificationResponse {
  id: number;
  title: string;
  message: string;
  notificationType: NotificationType;
  isRead: boolean;
  reminderId: number | null;
  reminderTitle?: string | null;
  taskId?: number | null;
  taskTitle?: string | null;
  goalId?: number | null;
  goalTitle?: string | null;
  milestoneId?: number | null;
  milestoneTitle?: string | null;
  habitId?: number | null;
  habitName?: string | null;
  createdAt: string;
}

export interface NotificationPageResponse {
  content: NotificationResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface UnreadCountResponse {
  unreadCount: number;
}

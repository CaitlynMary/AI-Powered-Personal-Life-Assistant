import {
  DashboardGoal,
  DashboardHabit,
  DashboardReminder,
  DashboardStat,
  DashboardTask,
} from '../../../shared/models/dashboard.models';

export type ApiTaskStatus = 'TODO' | 'IN_PROGRESS' | 'COMPLETED' | 'OVERDUE' | 'MISSED' | 'CANCELLED';
export type ApiTaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type ApiGoalStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'OVERDUE' | 'CANCELLED';
export type ApiGoalType = 'YEARLY' | 'MONTHLY' | 'WEEKLY';
export type ApiReminderStatus = 'PENDING' | 'SENT' | 'CANCELLED';
export type ApiReminderType = 'GENERAL' | 'TASK' | 'GOAL' | 'HABIT' | 'CALENDAR';

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface TaskResponse {
  id: number;
  title: string;
  priority: ApiTaskPriority;
  status: ApiTaskStatus;
  dueDate: string | null;
}

export interface TaskStatisticsResponse {
  totalTasks: number;
  activeTasks: number;
  pendingTasks: number;
  inProgressTasks: number;
  completedTasks: number;
  overdueTasks: number;
  missedTasks: number;
  cancelledTasks: number;
}

export interface GoalResponse {
  id: number;
  title: string;
  goalType: ApiGoalType;
  status: ApiGoalStatus;
  progressPercentage: number | null;
}

export interface GoalStatisticsResponse {
  notStarted: number;
  inProgress: number;
}

export interface MilestoneResponse {
  id: number;
  goalId: number;
  title: string;
  targetDate: string;
  status: 'NOT_STARTED' | 'COMPLETED';
}

export interface HabitResponse {
  id: number;
  name: string;
  active: boolean;
}

export interface HabitLogResponse {
  id: number;
  habitId: number;
  completionDate: string;
  completed: boolean;
}

export interface HabitStreakResponse {
  currentStreak: number;
}

export interface ReminderResponse {
  id: number;
  title: string;
  reminderDateTime: string;
  status: ApiReminderStatus;
  reminderType: ApiReminderType;
}

export interface DashboardData {
  stats: DashboardStat[];
  tasks: DashboardTask[];
  goals: DashboardGoal[];
  habits: DashboardHabit[];
  reminders: DashboardReminder[];
}

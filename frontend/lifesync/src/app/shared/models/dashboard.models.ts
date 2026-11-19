/** UI-facing dashboard view models populated by the dashboard API adapter. */

export type TaskPriority = 'low' | 'medium' | 'high' | 'urgent';
export type DashboardTaskStatus = 'TODO' | 'IN_PROGRESS' | 'COMPLETED' | 'OVERDUE' | 'MISSED' | 'CANCELLED';
export type DashboardGoalStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'OVERDUE' | 'CANCELLED';

export interface DashboardStat {
  id: string;
  label: string;
  value: string;
  icon: string;
  tone: 'tasks' | 'goals' | 'habits' | 'reminders';
}

export interface DashboardTask {
  id: string;
  title: string;
  completed: boolean;
  priority: TaskPriority;
  status: DashboardTaskStatus;
  dueTime: string;
}

export interface DashboardGoal {
  id: string;
  title: string;
  progress: number;
  icon: string;
  status: DashboardGoalStatus;
}

export interface HabitDay {
  label: string;
  completed: boolean;
}

export interface DashboardHabit {
  id: string;
  name: string;
  streak: number;
  days: HabitDay[];
}

export interface DashboardReminder {
  id: string;
  title: string;
  when: string;
  icon: string;
  status: 'PENDING' | 'SENT' | 'CANCELLED';
}

export interface AiSuggestion {
  id: string;
  label: string;
}

export interface DashboardUser {
  displayName: string;
  avatarInitials: string;
}

export interface DashboardSnapshot {
  user: DashboardUser;
  greeting: string;
  dateLabel: string;
  subtitle: string;
  stats: DashboardStat[];
  tasks: DashboardTask[];
  goals: DashboardGoal[];
  habits: DashboardHabit[];
  reminders: DashboardReminder[];
  aiSuggestions: AiSuggestion[];
}

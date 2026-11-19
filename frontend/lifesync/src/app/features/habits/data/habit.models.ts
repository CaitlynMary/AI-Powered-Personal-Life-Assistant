export type HabitFrequency = 'DAILY' | 'WEEKLY' | 'MONTHLY';

export interface HabitResponse {
  id: number;
  name: string;
  description: string | null;
  frequency: HabitFrequency;
  targetDaysPerWeek: number;
  startDate: string;
  endDate: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface HabitRequest {
  name: string;
  description: string | null;
  frequency: HabitFrequency;
  targetDaysPerWeek: number;
  startDate: string;
  endDate: string | null;
}

export interface HabitLogRequest {
  completionDate: string;
  completed: boolean;
  notes: string | null;
}

export interface HabitCompletionRequest {
  completionDate: string;
  completed?: boolean;
  notes?: string | null;
}

export interface HabitLogResponse {
  id: number;
  habitId: number;
  completionDate: string;
  completed: boolean;
  notes: string | null;
  createdAt: string;
}

export interface HabitStreakResponse {
  currentStreak: number;
  longestStreak: number;
  totalCompletedDays: number;
  completionPercentage: number;
  periodUnit: string;
}

export interface HabitCalendarDay {
  date: string;
  eligible: boolean;
  completed: boolean;
  notes: string | null;
}

export interface HabitCalendarResponse {
  habitId: number;
  year: number;
  month: number;
  completedDays: number;
  eligibleDays: number;
  completionPercentage: number;
  days: HabitCalendarDay[];
}

export interface HabitStatisticsResponse {
  totalHabits: number;
  activeHabits: number;
  totalCompletedDays: number;
  completionsThisWeek: number;
  completionsThisMonth: number;
  weeklyConsistencyPercentage: number;
  monthlyConsistencyPercentage: number;
}

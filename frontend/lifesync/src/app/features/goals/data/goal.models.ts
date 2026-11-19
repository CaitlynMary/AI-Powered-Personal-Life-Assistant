export type GoalType = 'YEARLY' | 'MONTHLY' | 'WEEKLY';
export type GoalStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type GoalLifecycleStatus = GoalStatus | 'OVERDUE';
export type MilestoneStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type MilestoneLifecycleStatus = MilestoneStatus | 'OVERDUE';

export interface GoalResponse {
  id: number;
  title: string;
  description: string | null;
  goalType: GoalType;
  startDate: string;
  targetDate: string;
  status: GoalLifecycleStatus;
  progressPercentage: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface GoalRequest {
  title: string;
  description: string | null;
  goalType: GoalType;
  startDate: string;
  targetDate: string;
}

export interface MilestoneResponse {
  id: number;
  goalId: number;
  title: string;
  description: string | null;
  targetDate: string;
  status: MilestoneLifecycleStatus;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface MilestoneRequest {
  title: string;
  description: string | null;
  targetDate: string;
}

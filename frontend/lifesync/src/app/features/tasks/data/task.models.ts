export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'COMPLETED' | 'OVERDUE' | 'MISSED' | 'CANCELLED';
export type TaskUpdateStatus = Exclude<TaskStatus, 'OVERDUE'>;
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type RecurrenceType = 'NONE' | 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY';

export interface TaskResponse {
  id: number;
  title: string;
  description: string | null;
  categoryId: number | null;
  categoryName: string | null;
  goalId: number | null;
  milestoneId: number | null;
  priority: TaskPriority;
  status: TaskStatus;
  dueDate: string | null;
  startDate: string | null;
  estimatedDuration: number | null;
  actualDuration: number | null;
  completedAt: string | null;
  missedAt: string | null;
  recurring: boolean;
  recurrenceType: RecurrenceType;
  createdAt: string;
  updatedAt: string;
}

export interface TaskPageResponse {
  content: TaskResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface TaskQuery {
  page?: number;
  size?: number;
  sortBy?: 'createdAt' | 'updatedAt' | 'title' | 'dueDate' | 'priority' | 'status' | 'startDate';
  direction?: 'ASC' | 'DESC';
  status?: TaskStatus | '';
  priority?: TaskPriority | '';
  categoryId?: number | '';
  search?: string;
  dueFrom?: string;
  dueTo?: string;
}

export interface TaskRequest {
  title: string;
  description: string | null;
  categoryId: number | null;
  goalId: number | null;
  milestoneId: number | null;
  priority: TaskPriority;
  dueDate: string | null;
  startDate: string | null;
  estimatedDuration: number | null;
  recurring: boolean;
  recurrenceType: RecurrenceType;
}

export interface CategoryResponse {
  id: number;
  name: string;
  description: string | null;
  color: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CategoryRequest {
  name: string;
  description: string | null;
  color: string | null;
}

import { DashboardData } from '../../dashboard/data/dashboard-api.models';
import { DashboardTask } from '../../../shared/models/dashboard.models';

export interface ApiEnvelope<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface AiChatRequest {
  message: string;
  history: AiChatHistoryMessage[];
}

export interface AiChatHistoryMessage {
  role: 'USER' | 'ASSISTANT';
  content: string;
}

export interface AiDailyBriefingTask {
  id: number;
  title: string;
  status: 'TODO' | 'IN_PROGRESS' | 'COMPLETED' | 'OVERDUE' | 'MISSED' | 'CANCELLED';
  priority: AiTaskPriority;
  dueDate: string | null;
}

export interface AiDailyBriefingEvent {
  id: number;
  title: string;
  eventType: string;
  startDateTime: string;
  endDateTime: string;
}

export interface AiDailyBriefingReminder {
  id: number;
  title: string;
  reminderDateTime: string;
  status: string;
  reminderType: string;
}

export interface AiDailyBriefing {
  date: string;
  pendingTasks: number;
  overdueTasks: number;
  missedTasks: number;
  tasksDueToday: number;
  highPriorityTasks: number;
  upcomingDeadlines: AiDailyBriefingTask[];
  eventsToday: AiDailyBriefingEvent[];
  upcomingReminderCount: number;
  upcomingReminders: AiDailyBriefingReminder[];
  activeHabits: number;
  habitCompletionsThisWeek: number;
  weeklyHabitConsistencyPercentage: number;
}

export interface DailyPlanRequest {
  date: string;
  dayStartTime: string;
  dayEndTime: string;
}

export interface DailyTimeWindow {
  startTime: string;
  endTime: string;
}

export type AiTaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export interface DailyPlanSuggestion {
  taskId: number;
  taskTitle: string;
  startTime: string;
  endTime: string;
  priority: AiTaskPriority;
  rationale: string;
}

export interface DailyPlanResponse {
  date: string;
  summary: string;
  availableWindows: DailyTimeWindow[];
  suggestions: DailyPlanSuggestion[];
  warnings: string[];
}

export interface DailyBriefing {
  tasksDueToday: number;
  completedTasks: number;
  pendingTasks: number;
  highPriorityTasks: number;
  topPriorityTask: DashboardTask | null;
  upcomingReminders: number;
  nextReminderTitle: string | null;
}

export interface ChatMessage {
  id: number;
  role: 'USER' | 'ASSISTANT';
  content: string;
  state: 'READY' | 'PENDING' | 'ERROR';
  retryPrompt?: string;
  retryHistory?: AiChatHistoryMessage[];
}

export function createDailyBriefing(data: DashboardData): DailyBriefing {
  const completedTasks = data.tasks.filter((task) => task.status === 'COMPLETED').length;
  const pendingTasks = data.tasks.filter(
    (task) => task.status === 'TODO' || task.status === 'IN_PROGRESS' || task.status === 'OVERDUE',
  );
  const priorityRank: Record<DashboardTask['priority'], number> = {
    urgent: 0,
    high: 1,
    medium: 2,
    low: 3,
  };
  const topPriorityTask =
    [...pendingTasks].sort((a, b) => priorityRank[a.priority] - priorityRank[b.priority])[0] ??
    null;

  return {
    tasksDueToday: data.tasks.length,
    completedTasks,
    pendingTasks: pendingTasks.length,
    highPriorityTasks: pendingTasks.filter(
      (task) => task.priority === 'urgent' || task.priority === 'high',
    ).length,
    topPriorityTask,
    upcomingReminders: data.reminders.length,
    nextReminderTitle: data.reminders[0]?.title ?? null,
  };
}

export function isDailyPlanResponse(
  value: unknown,
  requestedDate: string,
): value is DailyPlanResponse {
  if (!value || typeof value !== 'object') return false;
  const plan = value as Partial<DailyPlanResponse>;
  if (
    plan.date !== requestedDate ||
    typeof plan.summary !== 'string' ||
    !plan.summary.trim() ||
    !Array.isArray(plan.availableWindows) ||
    !Array.isArray(plan.suggestions) ||
    !Array.isArray(plan.warnings) ||
    !plan.warnings.every((warning) => typeof warning === 'string')
  ) {
    return false;
  }

  const validTime = (time: unknown): time is string =>
    typeof time === 'string' && /^(?:[01]\d|2[0-3]):[0-5]\d(?::[0-5]\d)?$/.test(time);
  const toSeconds = (time: string) => {
    const [hours, minutes, seconds = '0'] = time.split(':');
    return Number(hours) * 3600 + Number(minutes) * 60 + Number(seconds);
  };
  const windows = plan.availableWindows as DailyTimeWindow[];
  if (
    !windows.every(
      (window) =>
        window !== null &&
        typeof window === 'object' &&
        validTime(window.startTime) &&
        validTime(window.endTime) &&
        toSeconds(window.startTime) < toSeconds(window.endTime),
    )
  ) {
    return false;
  }

  const suggestions = plan.suggestions as DailyPlanSuggestion[];
  if (
    !suggestions.every(
      (item) =>
        item !== null &&
        typeof item === 'object' &&
        Number.isSafeInteger(item.taskId) &&
        item.taskId > 0 &&
        typeof item.taskTitle === 'string' &&
        item.taskTitle.trim().length > 0 &&
        typeof item.rationale === 'string' &&
        ['LOW', 'MEDIUM', 'HIGH', 'URGENT'].includes(item.priority) &&
        validTime(item.startTime) &&
        validTime(item.endTime) &&
        toSeconds(item.startTime) < toSeconds(item.endTime) &&
        windows.some(
          (window) =>
            toSeconds(item.startTime) >= toSeconds(window.startTime) &&
            toSeconds(item.endTime) <= toSeconds(window.endTime),
        ),
    )
  ) {
    return false;
  }

  const sorted = [...suggestions].sort((a, b) => toSeconds(a.startTime) - toSeconds(b.startTime));
  if (new Set(suggestions.map((item) => item.taskId)).size !== suggestions.length) {
    return false;
  }
  return sorted.every(
    (item, index) =>
      index === 0 || toSeconds(sorted[index - 1].endTime) <= toSeconds(item.startTime),
  );
}

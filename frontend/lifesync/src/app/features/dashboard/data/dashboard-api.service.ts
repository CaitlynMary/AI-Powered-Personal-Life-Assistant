import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map, of, switchMap } from 'rxjs';
import { API_BASE_URL } from '../../../core/api.config';
import { RegionalDateService } from '../../../core/services/regional-date.service';
import {
  ApiGoalType,
  ApiTaskPriority,
  DashboardData,
  GoalResponse,
  GoalStatisticsResponse,
  HabitLogResponse,
  HabitResponse,
  HabitStreakResponse,
  MilestoneResponse,
  PageResponse,
  ReminderResponse,
  TaskResponse,
  TaskStatisticsResponse,
} from './dashboard-api.models';
import {
  DashboardGoal,
  DashboardHabit,
  DashboardReminder,
  DashboardStat,
  DashboardTask,
} from '../../../shared/models/dashboard.models';

const PAGE_SIZE = 100;

@Injectable({ providedIn: 'root' })
export class DashboardApiService {
  private readonly http = inject(HttpClient);
  private readonly regionalDate = inject(RegionalDateService);

  loadDashboard(): Observable<DashboardData> {
    const now = new Date();
    const startOfToday = new Date(now);
    startOfToday.setHours(0, 0, 0, 0);
    const endOfToday = new Date(now);
    endOfToday.setHours(23, 59, 59, 999);

    const taskParams = new HttpParams()
      .set('page', '0')
      .set('size', PAGE_SIZE)
      .set('sortBy', 'dueDate')
      .set('direction', 'ASC')
      .set('dueFrom', this.localDateTime(startOfToday))
      .set('dueTo', this.localDateTime(endOfToday, true));
    const reminderParams = new HttpParams().set('page', '0').set('size', PAGE_SIZE);

    return forkJoin({
      tasks: this.http.get<PageResponse<TaskResponse>>(`${API_BASE_URL}/api/tasks`, {
        params: taskParams,
      }),
      taskStats: this.http.get<TaskStatisticsResponse>(
        `${API_BASE_URL}/api/tasks/statistics`,
      ),
      goals: this.http.get<GoalResponse[]>(`${API_BASE_URL}/api/goals`),
      goalStats: this.http.get<GoalStatisticsResponse>(
        `${API_BASE_URL}/api/goals/statistics`,
      ),
      habits: this.http.get<HabitResponse[]>(`${API_BASE_URL}/api/habits`),
      reminders: this.http.get<PageResponse<ReminderResponse>>(
        `${API_BASE_URL}/api/reminders/upcoming`,
        { params: reminderParams },
      ),
    }).pipe(
      switchMap(({ tasks, taskStats, goals, goalStats, habits, reminders }) =>
        forkJoin({
          goals: this.loadGoals(goals),
          habits: this.loadHabits(habits),
        }).pipe(
          map(({ goals: dashboardGoals, habits: dashboardHabits }) => ({
            stats: this.buildStats(
              taskStats,
              goalStats,
              dashboardHabits,
              reminders.totalElements,
            ),
            tasks: tasks.content
              .filter((task) => task.status !== 'CANCELLED' && task.status !== 'MISSED')
              .map((task) => this.toDashboardTask(task)),
            goals: dashboardGoals,
            habits: dashboardHabits,
            reminders: reminders.content.map((reminder) =>
              this.toDashboardReminder(reminder),
            ),
          })),
        ),
      ),
    );
  }

  setTaskCompleted(taskId: string, completed: boolean): Observable<TaskResponse> {
    const taskIdNumber = Number(taskId);
    if (!Number.isSafeInteger(taskIdNumber) || taskIdNumber <= 0) {
      throw new Error('Task ID must be a positive integer.');
    }

    if (completed) {
      return this.http.patch<TaskResponse>(
        `${API_BASE_URL}/api/tasks/${taskIdNumber}/complete`,
        null,
      );
    }

    return this.http.patch<TaskResponse>(
      `${API_BASE_URL}/api/tasks/${taskIdNumber}/status`,
      { status: 'TODO' },
    );
  }

  private loadGoals(goals: GoalResponse[]): Observable<DashboardGoal[]> {
    if (goals.length === 0) {
      return of([]);
    }

    return forkJoin(
      goals.map((goal) =>
        this.http
          .get<MilestoneResponse[]>(
            `${API_BASE_URL}/api/goals/${goal.id}/milestones`,
          )
          .pipe(map((milestones) => this.toDashboardGoal(goal, milestones))),
      ),
    );
  }

  private loadHabits(habits: HabitResponse[]): Observable<DashboardHabit[]> {
    const activeHabits = habits.filter((habit) => habit.active);
    if (activeHabits.length === 0) {
      return of([]);
    }

    return forkJoin(
      activeHabits.map((habit) =>
        forkJoin({
          logs: this.http.get<HabitLogResponse[]>(
            `${API_BASE_URL}/api/habits/${habit.id}/logs`,
          ),
          streak: this.http.get<HabitStreakResponse>(
            `${API_BASE_URL}/api/habits/${habit.id}/streak`,
          ),
        }).pipe(
          map(({ logs, streak }) => this.toDashboardHabit(habit, logs, streak)),
        ),
      ),
    );
  }

  private buildStats(
    taskStats: TaskStatisticsResponse,
    goalStats: GoalStatisticsResponse,
    habits: DashboardHabit[],
    upcomingReminderCount: number,
  ): DashboardStat[] {
    const currentStreak = habits.reduce(
      (longest, habit) => Math.max(longest, habit.streak),
      0,
    );

    return [
      {
        id: 'active-tasks',
        label: 'Active Tasks',
        value: String(taskStats.activeTasks),
        icon: 'list-checks',
        tone: 'tasks',
      },
      {
        id: 'pending-tasks',
        label: 'Tasks Pending',
        value: String(taskStats.pendingTasks),
        icon: 'square-check',
        tone: 'tasks',
      },
      {
        id: 'in-progress-tasks',
        label: 'In Progress',
        value: String(taskStats.inProgressTasks),
        icon: 'loader-circle',
        tone: 'tasks',
      },
      {
        id: 'completed-tasks',
        label: 'Completed',
        value: String(taskStats.completedTasks),
        icon: 'circle-check',
        tone: 'tasks',
      },
      {
        id: 'overdue-tasks',
        label: 'Overdue',
        value: String(taskStats.overdueTasks),
        icon: 'clock-alert',
        tone: 'tasks',
      },
      {
        id: 'missed-tasks',
        label: 'Missed',
        value: String(taskStats.missedTasks),
        icon: 'circle-x',
        tone: 'tasks',
      },
      {
        id: 'active-goals',
        label: 'Active Goals',
        value: String(goalStats.notStarted + goalStats.inProgress),
        icon: 'target',
        tone: 'goals',
      },
      {
        id: 'habit-streak',
        label: 'Habit Streak',
        value: `${currentStreak} day${currentStreak === 1 ? '' : 's'}`,
        icon: 'flame',
        tone: 'habits',
      },
      {
        id: 'upcoming-reminders',
        label: 'Upcoming Reminders',
        value: String(upcomingReminderCount),
        icon: 'calendar-clock',
        tone: 'reminders',
      },
    ];
  }

  private toDashboardTask(task: TaskResponse): DashboardTask {
    return {
      id: String(task.id),
      title: task.title,
      completed: task.status === 'COMPLETED',
      status: task.status,
      priority: this.taskPriority(task.priority),
      dueTime: task.dueDate ? this.formatDateTime(task.dueDate) : 'No due date',
    };
  }

  private toDashboardGoal(
    goal: GoalResponse,
    milestones: MilestoneResponse[],
  ): DashboardGoal {
    const completedMilestones = milestones.filter(
      (milestone) => milestone.status === 'COMPLETED',
    ).length;
    const progress =
      milestones.length > 0
        ? Math.round((completedMilestones / milestones.length) * 100)
        : (goal.progressPercentage ?? 0);

    return {
      id: String(goal.id),
      title: goal.title,
      progress: Math.max(0, Math.min(100, progress)),
      status: goal.status,
      icon: this.goalIcon(goal.goalType),
    };
  }

  private toDashboardHabit(
    habit: HabitResponse,
    logs: HabitLogResponse[],
    streak: HabitStreakResponse,
  ): DashboardHabit {
    const today = new Date();
    const completedDates = new Set(
      logs.filter((log) => log.completed).map((log) => log.completionDate),
    );
    const days = Array.from({ length: 7 }, (_, index) => {
      const date = new Date(today);
      date.setHours(0, 0, 0, 0);
      date.setDate(today.getDate() - 6 + index);
      return {
        label: this.regionalDate.formatDateOnly(date, { weekday: 'short' })
          .slice(0, 1),
        completed: completedDates.has(this.localDate(date)),
      };
    });

    return {
      id: String(habit.id),
      name: habit.name,
      streak: streak.currentStreak,
      days,
    };
  }

  private toDashboardReminder(reminder: ReminderResponse): DashboardReminder {
    return {
      id: String(reminder.id),
      title: reminder.title,
      when: this.formatDateTime(reminder.reminderDateTime),
      icon: this.reminderIcon(reminder.reminderType),
      status: reminder.status,
    };
  }

  private goalIcon(type: ApiGoalType): string {
    switch (type) {
      case 'MONTHLY':
        return 'calendar-days';
      case 'WEEKLY':
        return 'layers';
      default:
        return 'target';
    }
  }

  private taskPriority(priority: ApiTaskPriority): DashboardTask['priority'] {
    switch (priority) {
      case 'LOW':
        return 'low';
      case 'MEDIUM':
        return 'medium';
      case 'HIGH':
        return 'high';
      default:
        return 'urgent';
    }
  }

  private reminderIcon(type: ReminderResponse['reminderType']): string {
    switch (type) {
      case 'TASK':
        return 'square-check';
      case 'GOAL':
        return 'target';
      case 'HABIT':
        return 'repeat';
      case 'CALENDAR':
        return 'calendar-days';
      default:
        return 'calendar-clock';
    }
  }

  private formatDateTime(value: string): string {
    return this.regionalDate.formatDateTime(value, {
      dateStyle: 'medium',
      timeStyle: 'short',
    });
  }

  private localDateTime(date: Date, endOfRange = false): string {
    const fraction = endOfRange
      ? '999999999'
      : String(date.getMilliseconds()).padStart(3, '0');
    return `${this.localDate(date)}T${this.pad(date.getHours())}:${this.pad(date.getMinutes())}:${this.pad(date.getSeconds())}.${fraction}`;
  }

  private localDate(date: Date): string {
    return `${date.getFullYear()}-${this.pad(date.getMonth() + 1)}-${this.pad(date.getDate())}`;
  }

  private pad(value: number): string {
    return String(value).padStart(2, '0');
  }
}

import { DashboardData } from '../../dashboard/data/dashboard-api.models';
import { createDailyBriefing, DailyPlanResponse, isDailyPlanResponse } from './ai-assistant.models';

function dashboardData(): DashboardData {
  return {
    stats: [],
    tasks: [
      {
        id: '1',
        title: 'Review proposal',
        completed: false,
        priority: 'high',
        status: 'TODO',
        dueTime: '10:00 AM',
      },
      {
        id: '2',
        title: 'Send update',
        completed: false,
        priority: 'urgent',
        status: 'IN_PROGRESS',
        dueTime: '11:00 AM',
      },
      {
        id: '3',
        title: 'Completed item',
        completed: true,
        priority: 'low',
        status: 'COMPLETED',
        dueTime: 'Yesterday',
      },
    ],
    goals: [],
    habits: [],
    reminders: [
      {
        id: '9',
        title: 'Water break',
        when: 'Today, 1:00 PM',
        icon: 'calendar-clock',
        status: 'PENDING',
      },
    ],
  };
}

function validPlan(): DailyPlanResponse {
  return {
    date: '2026-11-13',
    summary: 'Complete the report first.',
    availableWindows: [{ startTime: '09:00:00', endTime: '12:00:00' }],
    suggestions: [
      {
        taskId: 1,
        taskTitle: 'Review proposal',
        startTime: '09:00:00',
        endTime: '10:00:00',
        priority: 'HIGH',
        rationale: 'It is high priority.',
      },
    ],
    warnings: [],
  };
}

describe('AI assistant data helpers', () => {
  it('derives only supported daily briefing facts from dashboard API records', () => {
    const briefing = createDailyBriefing(dashboardData());
    expect(briefing).toEqual({
      tasksDueToday: 3,
      completedTasks: 1,
      pendingTasks: 2,
      highPriorityTasks: 2,
      topPriorityTask: expect.objectContaining({ id: '2', title: 'Send update' }),
      upcomingReminders: 1,
      nextReminderTitle: 'Water break',
    });
  });

  it('accepts a structurally valid draft and rejects out-of-window or overlapping suggestions', () => {
    const plan = validPlan();
    expect(isDailyPlanResponse(plan, '2026-11-13')).toBe(true);

    expect(
      isDailyPlanResponse(
        {
          ...plan,
          suggestions: [{ ...plan.suggestions[0], startTime: '08:00:00' }],
        },
        '2026-11-13',
      ),
    ).toBe(false);

    expect(
      isDailyPlanResponse(
        {
          ...plan,
          suggestions: [
            ...plan.suggestions,
            {
              ...plan.suggestions[0],
              taskId: 2,
              startTime: '09:30:00',
              endTime: '10:30:00',
            },
          ],
        },
        '2026-11-13',
      ),
    ).toBe(false);
  });
});

import { DashboardSnapshot } from '../../../shared/models/dashboard.models';

/**
 * Stage 1 mock fixtures retained separately for comparison and local UI demos.
 * The dashboard now reads its stats, tasks, goals, habits, and reminders from
 * the authenticated backend; only its decorative copy and AI suggestions use these fixtures.
 */
export const DASHBOARD_MOCK: DashboardSnapshot = {
  user: {
    displayName: 'Cathy',
    avatarInitials: 'C',
  },
  greeting: 'Good Morning! 👋',
  dateLabel: 'Tuesday, 10 November 2026',
  subtitle: 'Your personalised plan for a productive and balanced day.',
  stats: [
    {
      id: 'pending-tasks',
      label: 'Tasks Pending',
      value: '12',
      icon: 'square-check',
      tone: 'tasks',
    },
    {
      id: 'active-goals',
      label: 'Active Goals',
      value: '3',
      icon: 'target',
      tone: 'goals',
    },
    {
      id: 'habit-streak',
      label: 'Habit Streak',
      value: '5 days',
      icon: 'flame',
      tone: 'habits',
    },
    {
      id: 'upcoming-reminders',
      label: 'Upcoming Reminders',
      value: '4',
      icon: 'calendar-clock',
      tone: 'reminders',
    },
  ],
  tasks: [
    {
      id: 't1',
      title: 'Finish project proposal',
      completed: true,
      priority: 'high',
      status: 'COMPLETED',
      dueTime: '10:00 AM',
    },
    {
      id: 't2',
      title: 'Team standup meeting',
      completed: false,
      priority: 'medium',
      status: 'TODO',
      dueTime: '11:30 AM',
    },
    {
      id: 't3',
      title: 'Review design mockups',
      completed: false,
      priority: 'high',
      status: 'IN_PROGRESS',
      dueTime: '2:00 PM',
    },
    {
      id: 't4',
      title: 'Grocery shopping',
      completed: false,
      priority: 'low',
      status: 'TODO',
      dueTime: '5:00 PM',
    },
  ],
  goals: [
    {
      id: 'g1',
      title: 'Complete Online Course',
      progress: 75,
      icon: 'laptop',
      status: 'IN_PROGRESS',
    },
    {
      id: 'g2',
      title: 'Save for Vacation',
      progress: 60,
      icon: 'layers',
      status: 'IN_PROGRESS',
    },
    {
      id: 'g3',
      title: 'Learn Spanish',
      progress: 80,
      icon: 'graduation-cap',
      status: 'IN_PROGRESS',
    },
  ],
  habits: [
    {
      id: 'h1',
      name: 'Exercise',
      streak: 5,
      days: [
        { label: 'M', completed: true },
        { label: 'T', completed: true },
        { label: 'W', completed: true },
        { label: 'T', completed: true },
        { label: 'F', completed: true },
        { label: 'S', completed: false },
        { label: 'S', completed: false },
      ],
    },
    {
      id: 'h2',
      name: 'Read',
      streak: 3,
      days: [
        { label: 'M', completed: true },
        { label: 'T', completed: false },
        { label: 'W', completed: true },
        { label: 'T', completed: true },
        { label: 'F', completed: true },
        { label: 'S', completed: false },
        { label: 'S', completed: false },
      ],
    },
    {
      id: 'h3',
      name: 'Drink Water',
      streak: 7,
      days: [
        { label: 'M', completed: true },
        { label: 'T', completed: true },
        { label: 'W', completed: true },
        { label: 'T', completed: true },
        { label: 'F', completed: true },
        { label: 'S', completed: true },
        { label: 'S', completed: true },
      ],
    },
    {
      id: 'h4',
      name: 'Meditation',
      streak: 2,
      days: [
        { label: 'M', completed: false },
        { label: 'T', completed: true },
        { label: 'W', completed: false },
        { label: 'T', completed: true },
        { label: 'F', completed: true },
        { label: 'S', completed: false },
        { label: 'S', completed: false },
      ],
    },
  ],
  reminders: [
    {
      id: 'r1',
      title: "Doctor's Appointment",
      when: 'Today, 3:00 PM',
      icon: 'stethoscope',
      status: 'PENDING',
    },
    {
      id: 'r2',
      title: 'Pay Electricity Bill',
      when: 'Tomorrow, 9:00 AM',
      icon: 'zap',
      status: 'PENDING',
    },
    {
      id: 'r3',
      title: "Mom's Birthday",
      when: 'Nov 15',
      icon: 'cake',
      status: 'PENDING',
    },
  ],
  aiSuggestions: [
    { id: 'a1', label: 'Plan my day' },
    { id: 'a2', label: 'Break a goal into steps' },
    { id: 'a3', label: 'Create a study plan' },
  ],
};

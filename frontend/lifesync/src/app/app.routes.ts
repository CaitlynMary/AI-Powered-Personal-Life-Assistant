import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth/auth.guard';
import { MainLayout } from './core/layout/main-layout/main-layout';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () =>
      import('./features/landing/landing.page').then((m) => m.LandingPage),
    title: 'LifeSync · Plan Focus Grow',
  },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login/login.page').then((m) => m.LoginPage),
    title: 'Sign in · LifeSync',
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/register/register.page').then((m) => m.RegisterPage),
    title: 'Create account · LifeSync',
  },
  {
    path: '',
    component: MainLayout,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.page').then((m) => m.DashboardPage),
        title: 'Dashboard · LifeSync',
      },
      {
        path: 'tasks',
        loadComponent: () => import('./features/tasks/tasks.page').then((m) => m.TasksPage),
        data: { title: 'Tasks' },
        title: 'Tasks · LifeSync',
      },
      {
        path: 'goals',
        loadComponent: () => import('./features/goals/goals.page').then((m) => m.GoalsPage),
        title: 'Goals · LifeSync',
      },
      {
        path: 'habits',
        loadComponent: () => import('./features/habits/habits.page').then((m) => m.HabitsPage),
        title: 'Habits · LifeSync',
      },
      {
        path: 'calendar',
        loadComponent: () =>
          import('./features/calendar/calendar.page').then((m) => m.CalendarPage),
        title: 'Calendar · LifeSync',
      },
      {
        path: 'planner',
        loadComponent: () =>
          import('./features/planner/planner.page').then((m) => m.PlannerPage),
        title: 'Planner · LifeSync',
      },
      {
        path: 'notes',
        loadComponent: () => import('./features/notes/notes.page').then((m) => m.NotesPage),
        title: 'Notes · LifeSync',
      },
      {
        path: 'journal',
        loadComponent: () => import('./features/journal/journal.page').then((m) => m.JournalPage),
        title: 'Journal · LifeSync',
      },
      {
        path: 'ai-assistant',
        loadComponent: () =>
          import('./features/ai-assistant/ai-assistant.page').then((m) => m.AiAssistantPage),
        title: 'AI Assistant · LifeSync',
      },
      {
        path: 'reminders',
        loadComponent: () =>
          import('./features/reminders/reminders.page').then((m) => m.RemindersPage),
        title: 'Reminders · LifeSync',
      },
      {
        path: 'notifications',
        loadComponent: () =>
          import('./features/notifications/notifications.page').then((m) => m.NotificationsPage),
        title: 'Notifications · LifeSync',
      },
      {
        path: 'settings',
        loadComponent: () =>
          import('./features/settings/settings.page').then((m) => m.SettingsPage),
        title: 'Settings · LifeSync',
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];

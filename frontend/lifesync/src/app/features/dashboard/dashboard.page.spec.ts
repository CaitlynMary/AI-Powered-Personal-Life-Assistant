import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WritableSignal, signal } from '@angular/core';
import { provideRouter, Router } from '@angular/router';
import {
  LucideArrowRight,
  LucideCheck,
  LucideMessageCircle,
  LucidePlus,
  provideLucideIcons,
} from '@lucide/angular';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { UserProfile } from '../../core/auth/auth.models';
import { DashboardPage } from './dashboard.page';
import { DashboardApiService } from './data/dashboard-api.service';
import { DashboardData } from './data/dashboard-api.models';

const dashboardData: DashboardData = {
  stats: [],
  tasks: [],
  goals: [],
  habits: [],
  reminders: [],
};

const userProfile = (fullName: string): UserProfile => ({
  id: 1,
  fullName,
  email: 'user@example.com',
  phoneNumber: null,
  active: true,
  createdAt: '2026-01-01T00:00:00Z',
});

describe('DashboardPage', () => {
  let fixture: ComponentFixture<DashboardPage>;
  let routerNavigate: ReturnType<typeof vi.fn>;
  let loadDashboard: ReturnType<typeof vi.fn>;
  let profileState: WritableSignal<UserProfile | null>;

  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2026, 10, 17, 9));
    loadDashboard = vi.fn().mockReturnValue(of(dashboardData));
    profileState = signal<UserProfile | null>(null);
    TestBed.configureTestingModule({
      imports: [DashboardPage],
      providers: [
        provideRouter([]),
        provideLucideIcons(LucideArrowRight, LucideCheck, LucideMessageCircle, LucidePlus),
        {
          provide: AuthService,
          useValue: { currentUserProfile: profileState.asReadonly() },
        },
        { provide: DashboardApiService, useValue: { loadDashboard } },
      ],
    });
    routerNavigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(DashboardPage);
  });

  afterEach(() => {
    fixture.destroy();
    vi.useRealTimers();
  });

  function renderAt(date: Date, profile: UserProfile | null): void {
    fixture.destroy();
    vi.setSystemTime(date);
    profileState.set(profile);
    fixture = TestBed.createComponent(DashboardPage);
  }

  it('uses the logged-in user name and local time for each greeting period', () => {
    renderAt(new Date(2026, 10, 17, 5), userProfile('Caitlyn Mary'));
    expect(fixture.componentInstance.greeting()).toBe('Good Morning, Caitlyn! ☀️');

    renderAt(new Date(2026, 10, 17, 11, 59), userProfile('Caitlyn Mary'));
    expect(fixture.componentInstance.greeting()).toBe('Good Morning, Caitlyn! ☀️');

    renderAt(new Date(2026, 10, 17, 12), userProfile('John Smith'));
    expect(fixture.componentInstance.greeting()).toBe('Good Afternoon, John! 🌤️');

    renderAt(new Date(2026, 10, 17, 16, 59), userProfile('John Smith'));
    expect(fixture.componentInstance.greeting()).toBe('Good Afternoon, John! 🌤️');

    renderAt(new Date(2026, 10, 17, 17), userProfile('Sarah Jones'));
    expect(fixture.componentInstance.greeting()).toBe('Good Evening, Sarah! 🌙');

    profileState.set(userProfile('Alex Morgan'));
    expect(fixture.componentInstance.greeting()).toBe('Good Evening, Alex! 🌙');

    renderAt(new Date(2026, 10, 18, 4, 59), userProfile('Sarah Jones'));
    expect(fixture.componentInstance.greeting()).toBe('Good Evening, Sarah! 🌙');
  });

  it('falls back to there when the profile name is missing', () => {
    renderAt(new Date(2026, 10, 17, 7), null);
    expect(fixture.componentInstance.greeting()).toBe('Good Morning, there! ☀️');

    profileState.set(userProfile('   '));
    expect(fixture.componentInstance.greeting()).toBe('Good Morning, there! ☀️');
  });

  it('updates at period boundaries and handles midnight while open', () => {
    renderAt(new Date(2026, 10, 17, 11, 59, 59), userProfile('Caitlyn Mary'));
    expect(fixture.componentInstance.greeting()).toBe('Good Morning, Caitlyn! ☀️');
    const initialQuote = fixture.componentInstance.dailyInspiration().quote;

    vi.advanceTimersByTime(1000);
    expect(fixture.componentInstance.greeting()).toBe('Good Afternoon, Caitlyn! 🌤️');

    vi.advanceTimersByTime(5 * 60 * 60 * 1000);
    expect(fixture.componentInstance.greeting()).toBe('Good Evening, Caitlyn! 🌙');

    vi.advanceTimersByTime(7 * 60 * 60 * 1000);
    expect(fixture.componentInstance.greeting()).toBe('Good Evening, Caitlyn! 🌙');
    const nextDayQuote = fixture.componentInstance.dailyInspiration().quote;
    expect(nextDayQuote).not.toBe(initialQuote);

    vi.advanceTimersByTime(5 * 60 * 60 * 1000);
    expect(fixture.componentInstance.greeting()).toBe('Good Morning, Caitlyn! ☀️');
    expect(fixture.componentInstance.dailyInspiration().quote).toBe(nextDayQuote);
  });

  it('navigates from the keyboard-accessible Add Task button to the requested create form', () => {
    fixture.detectChanges();

    const addTaskButton = fixture.nativeElement.querySelector(
      'app-todays-tasks button',
    ) as HTMLButtonElement;
    expect(addTaskButton.type).toBe('button');
    expect(addTaskButton.textContent).toContain('Add Task');
    expect(fixture.nativeElement.textContent).toContain('No tasks scheduled for today.');

    addTaskButton.click();

    expect(routerNavigate).toHaveBeenCalledOnce();
    expect(routerNavigate).toHaveBeenCalledWith(['/tasks'], {
      queryParams: { openCreateTask: 'true' },
    });
    expect(loadDashboard).toHaveBeenCalledOnce();
  });

  it('clears loading on request failure and recovers when the dashboard is retried', () => {
    loadDashboard.mockReset();
    loadDashboard
      .mockReturnValueOnce(throwError(() => new Error('offline')))
      .mockReturnValueOnce(of(dashboardData));
    fixture.componentInstance.loadDashboard();
    fixture.detectChanges();

    expect(fixture.componentInstance.loading()).toBe(false);
    expect(fixture.nativeElement.textContent).toContain(
      'Could not load dashboard data. Please try again.',
    );
    expect(fixture.nativeElement.textContent).not.toContain('No tasks scheduled for today.');

    const retryButton = fixture.nativeElement.querySelector(
      '[role="alert"] button',
    ) as HTMLButtonElement;
    retryButton.click();
    fixture.detectChanges();

    expect(loadDashboard).toHaveBeenCalledTimes(2);
    expect(fixture.componentInstance.loading()).toBe(false);
    expect(fixture.nativeElement.textContent).not.toContain(
      'Could not load dashboard data. Please try again.',
    );
    expect(fixture.nativeElement.textContent).toContain('No tasks scheduled for today.');
  });
});

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TimeoutError } from 'rxjs';
import { vi } from 'vitest';
import { API_BASE_URL } from '../../../core/api.config';
import { AI_CHAT_TIMEOUT_MS, AiAssistantApiService } from './ai-assistant-api.service';

describe('AiAssistantApiService', () => {
  let service: AiAssistantApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AiAssistantApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify({ ignoreCancelled: true });
    vi.useRealTimers();
  });

  it('sends chat requests to the authenticated backend and unwraps its response envelope', () => {
    let response: string | undefined;
    service.chat('What can I focus on?').subscribe((value) => (response = value));
    const request = http.expectOne(`${API_BASE_URL}/api/ai/chat`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ message: 'What can I focus on?', history: [] });
    request.flush({
      success: true,
      message: 'AI response generated',
      data: 'Start with one task.',
    });

    expect(response).toBe('Start with one task.');
  });

  it('loads the factual daily briefing from the authenticated backend', () => {
    let response: unknown;
    service.dailyBriefing().subscribe((value) => (response = value));
    const request = http.expectOne(`${API_BASE_URL}/api/ai/daily-briefing`);
    expect(request.request.method).toBe('GET');
    const briefing = {
      date: '2026-11-13',
      pendingTasks: 3,
      overdueTasks: 1,
      missedTasks: 1,
      tasksDueToday: 2,
      highPriorityTasks: 1,
      upcomingDeadlines: [],
      eventsToday: [],
      upcomingReminderCount: 0,
      upcomingReminders: [],
      activeHabits: 2,
      habitCompletionsThisWeek: 5,
      weeklyHabitConsistencyPercentage: 70,
    };
    request.flush(briefing);
    expect(response).toEqual(briefing);
  });

  it('posts the explicit daily availability and returns the structured draft', () => {
    let response: unknown;
    service
      .createDailyPlan({
        date: '2026-11-13',
        dayStartTime: '09:00',
        dayEndTime: '15:00',
      })
      .subscribe((value) => (response = value));
    const request = http.expectOne(`${API_BASE_URL}/api/ai/daily-plan`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      date: '2026-11-13',
      dayStartTime: '09:00',
      dayEndTime: '15:00',
    });
    const draft = {
      date: '2026-11-13',
      summary: 'Start with the report.',
      availableWindows: [{ startTime: '09:00:00', endTime: '15:00:00' }],
      suggestions: [],
      warnings: [],
    };
    request.flush({
      success: true,
      message: 'Daily plan suggestions are ready for review',
      data: draft,
    });
    expect(response).toEqual(draft);
  });

  it('rejects unsuccessful or empty response envelopes instead of fabricating data', () => {
    let error: unknown;
    service.chat('Hello').subscribe({ error: (value: unknown) => (error = value) });
    http.expectOne(`${API_BASE_URL}/api/ai/chat`).flush({
      success: false,
      message: 'AI unavailable',
      data: null,
    });
    expect(error).toBeInstanceOf(Error);
    expect((error as Error).message).toContain('AI unavailable');
  });

  it('times out a chat request that does not receive a response and cancels its HTTP request', () => {
    vi.useFakeTimers();
    let error: unknown;
    service.chat('Hello').subscribe({ error: (value: unknown) => (error = value) });
    const request = http.expectOne(`${API_BASE_URL}/api/ai/chat`);

    vi.advanceTimersByTime(AI_CHAT_TIMEOUT_MS);

    expect(error).toBeInstanceOf(TimeoutError);
    expect(request.cancelled).toBe(true);
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';
import { AiAssistantApiService } from './data/ai-assistant-api.service';
import { AiDailyBriefing } from './data/ai-assistant.models';
import { AiChatPanel } from './components/ai-chat-panel';
import { AiAssistantPage } from './ai-assistant.page';

const dailyBriefing: AiDailyBriefing = {
  date: '2026-11-13',
  pendingTasks: 2,
  overdueTasks: 1,
  missedTasks: 1,
  tasksDueToday: 1,
  highPriorityTasks: 1,
  upcomingDeadlines: [
    {
      id: 18,
      title: 'Finish the presentation',
      priority: 'URGENT',
      status: 'IN_PROGRESS',
      dueDate: '2026-11-13T14:00:00',
    },
  ],
  eventsToday: [
    {
      id: 30,
      title: 'Team check-in',
      eventType: 'MEETING',
      startDateTime: '2026-11-13T10:00:00',
      endDateTime: '2026-11-13T10:30:00',
    },
  ],
  upcomingReminders: [
    {
      id: 22,
      title: 'Water plants',
      reminderDateTime: '2026-11-14T09:00:00',
      reminderType: 'GENERAL',
      status: 'PENDING',
    },
  ],
  upcomingReminderCount: 1,
  activeHabits: 3,
  habitCompletionsThisWeek: 10,
  weeklyHabitConsistencyPercentage: 80,
};

describe('AiAssistantPage', () => {
  let fixture: ComponentFixture<AiAssistantPage>;
  let component: AiAssistantPage;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: AiAssistantApiService,
          useValue: {
            dailyBriefing: () => of(dailyBriefing),
            chat: () => of('A useful next step.'),
          },
        },
      ],
    });
    fixture = TestBed.createComponent(AiAssistantPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('shows actual account facts and switches between chat and briefing', () => {
    expect(component.briefing()?.pendingTasks).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('Finish the presentation');
    expect(fixture.nativeElement.textContent).toContain('Team check-in');
    expect(fixture.nativeElement.textContent).toContain('Water plants');

    component.selectTab('BRIEFING');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Your Daily Briefing');
    expect(fixture.nativeElement.textContent).toContain('80%');
  });

  it('keeps the in-session chat mounted when switching assistant views', () => {
    const chat = fixture.debugElement.query(By.directive(AiChatPanel))
      .componentInstance as AiChatPanel;
    chat.askSuggestion('Help me get started.');
    expect(chat.messages().map((message) => message.state)).toEqual(['READY', 'READY']);

    component.selectTab('BRIEFING');
    fixture.detectChanges();
    component.selectTab('CHAT');
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.directive(AiChatPanel)).componentInstance).toBe(chat);
  });
});

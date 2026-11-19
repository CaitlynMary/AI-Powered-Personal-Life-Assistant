import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal, ViewChild } from '@angular/core';
import { finalize } from 'rxjs';
import { readApiError } from '../../core/auth/auth-error';
import { RegionalDateService } from '../../core/services/regional-date.service';
import { AiAssistantApiService } from './data/ai-assistant-api.service';
import { AiDailyBriefing } from './data/ai-assistant.models';
import { AiChatPanel } from './components/ai-chat-panel';

type AssistantTab = 'CHAT' | 'BRIEFING';

@Component({
  selector: 'app-ai-assistant-page',
  standalone: true,
  imports: [CommonModule, AiChatPanel],
  templateUrl: './ai-assistant.page.html',
})
export class AiAssistantPage {
  private readonly api = inject(AiAssistantApiService);
  private readonly regionalDate = inject(RegionalDateService);
  @ViewChild(AiChatPanel) private chatPanel?: AiChatPanel;

  readonly activeTab = signal<AssistantTab>('CHAT');
  readonly briefing = signal<AiDailyBriefing | null>(null);
  readonly briefingLoading = signal(true);
  readonly briefingError = signal<string | null>(null);
  readonly currentDateLabel = computed(() =>
    this.regionalDate.formatDateOnly(new Date(), {
      weekday: 'long',
      month: 'long',
      day: 'numeric',
      year: 'numeric',
    }),
  );

  constructor() {
    this.refreshBriefing();
  }

  selectTab(tab: AssistantTab): void {
    this.activeTab.set(tab);
  }

  refreshBriefing(): void {
    this.briefingLoading.set(true);
    this.briefingError.set(null);
    this.api
      .dailyBriefing()
      .pipe(finalize(() => this.briefingLoading.set(false)))
      .subscribe({
        next: (data) => this.briefing.set(data),
        error: (error: unknown) => {
          this.briefingError.set(
            readApiError(error, 'Could not load today’s LifeSync briefing. Please try again.'),
          );
        },
      });
  }

  askForDailyFocus(): void {
    this.selectTab('CHAT');
    this.chatPanel?.askSuggestion(
      'What should I focus on today based on my tasks, calendar events, reminders and habits?',
    );
  }

  formatTime(value: string): string {
    return this.regionalDate.formatTime(value, { hour: 'numeric', minute: '2-digit' });
  }
}

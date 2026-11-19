import { Component, input, output } from '@angular/core';
import { LucideDynamicIcon } from '@lucide/angular';
import { DailyBriefing } from '../../../ai-assistant/data/ai-assistant.models';
import { AiSuggestion } from '../../../../shared/models/dashboard.models';
import { AiAssistantIllustration } from '../../../../shared/illustrations/ai-assistant-illustration';

@Component({
  selector: 'app-ai-assistant-card',
  standalone: true,
  imports: [LucideDynamicIcon, AiAssistantIllustration],
  template: `
    <section
      class="ls-card relative flex h-full flex-col overflow-hidden p-5 text-cream"
      style="background: linear-gradient(145deg, var(--ls-ai-from), var(--ls-ai-to));"
    >
      <div
        class="pointer-events-none absolute -right-8 -top-10 h-36 w-36 rounded-full bg-rose/20 blur-2xl"
      ></div>

      <div class="relative z-10 mb-3 flex items-start justify-between gap-3">
        <div>
          <p class="text-xs font-bold uppercase tracking-[0.14em] text-aqua">AI Assistant</p>
          <h2 class="mt-1 text-xl font-extrabold text-cream">Need a hand today?</h2>
          <p class="mt-2 max-w-xs text-sm font-medium text-cream/75">
            Chat with LifeSync AI or draft a day plan around your real commitments.
          </p>
        </div>
        <div class="h-28 w-28 shrink-0 sm:h-32 sm:w-32">
          <app-ai-assistant-illustration />
        </div>
      </div>

      @if (briefing(); as snapshot) {
        <div class="relative z-10 mb-3 rounded-2xl border border-white/10 bg-ls-surface/10 px-3 py-2.5">
          <p class="text-[10px] font-extrabold uppercase tracking-wide text-aqua">
            Your day at a glance · live data
          </p>
          <p class="mt-1 text-xs font-semibold text-cream">
            {{ snapshot.pendingTasks }} pending of {{ snapshot.tasksDueToday }} tasks due today
            <span class="text-cream/70">
              · {{ snapshot.upcomingReminders }} upcoming reminders</span
            >
          </p>
          @if (snapshot.topPriorityTask; as task) {
            <p class="mt-1 truncate text-[11px] text-cream/80">
              Suggested first focus: {{ task.title }} ({{ task.priority }})
            </p>
          }
        </div>
      }

      <div class="relative z-10 mt-auto space-y-3">
        <div class="flex flex-wrap gap-2">
          @for (suggestion of suggestions(); track suggestion.id) {
            <button
              type="button"
              class="rounded-full border border-cream/20 bg-ls-surface/10 px-3 py-1.5 text-xs font-semibold text-cream transition hover:bg-ls-surface/20"
              (click)="suggestionClick.emit(suggestion.id)"
            >
              {{ suggestion.label }}
            </button>
          }
        </div>

        <button
          type="button"
          class="inline-flex w-full items-center justify-center gap-2 rounded-2xl bg-lime px-4 py-3 text-sm font-extrabold text-plum transition hover:bg-mint"
          (click)="chatClick.emit()"
        >
          <svg lucideIcon="message-circle" [size]="18"></svg>
          Chat with LifeSync AI
          <svg lucideIcon="arrow-right" [size]="16"></svg>
        </button>
      </div>
    </section>
  `,
})
export class AiAssistantCard {
  readonly suggestions = input.required<AiSuggestion[]>();
  readonly briefing = input<DailyBriefing | null>(null);
  readonly suggestionClick = output<string>();
  readonly chatClick = output<void>();
}

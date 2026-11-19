import { CommonModule } from '@angular/common';
import {
  Component,
  DestroyRef,
  ElementRef,
  SecurityContext,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DomSanitizer } from '@angular/platform-browser';
import { Marked, Renderer } from 'marked';
import { TimeoutError, finalize } from 'rxjs';
import { readApiError } from '../../../core/auth/auth-error';
import { AiAssistantApiService } from '../data/ai-assistant-api.service';
import { AiChatHistoryMessage, ChatMessage } from '../data/ai-assistant.models';

const markdownBadgePattern =
  /\b(?:URGENT|HIGH|MEDIUM|LOW|TO\s+DO|IN\s+PROGRESS|COMPLETED|CANCELLED|PENDING|OVERDUE|TODAY|TOMORROW)\b|\b\d{4}-\d{2}-\d{2}\b/gi;

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

const markdownRenderer = new Renderer();
markdownRenderer.text = ({ text }) =>
  escapeHtml(text).replace(markdownBadgePattern, (label) => {
    const kind = /^\d{4}-\d{2}-\d{2}$/.test(label)
      ? 'date'
      : /^(urgent|high|medium|low)$/i.test(label)
        ? 'priority'
        : 'status';
    return `<span class="ai-markdown-badge ai-markdown-badge--${kind}">${label}</span>`;
  });
markdownRenderer.html = ({ text }) => escapeHtml(text);

const markdown = new Marked({
  breaks: true,
  gfm: true,
  renderer: markdownRenderer,
});

const SUGGESTED_QUESTIONS = [
  'What should I focus on today?',
  'Help me prioritize my next steps.',
  'How can I make a realistic plan for my day?',
];

@Component({
  selector: 'app-ai-chat-panel',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './ai-chat-panel.html',
  styleUrl: './ai-chat-panel.css',
})
export class AiChatPanel {
  private readonly formBuilder = inject(FormBuilder);
  private readonly api = inject(AiAssistantApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly sanitizer = inject(DomSanitizer);
  private nextId = 1;

  @ViewChild('conversation') private conversation?: ElementRef<HTMLElement>;

  readonly messages = signal<ChatMessage[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly suggestions = SUGGESTED_QUESTIONS;
  readonly form = this.formBuilder.nonNullable.group({
    message: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(4000)]],
  });

  submit(): void {
    if (this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    const prompt = this.form.controls.message.value.trim();
    this.form.reset({ message: '' });
    this.ask(prompt);
  }

  askSuggestion(prompt: string): void {
    if (!this.loading()) this.ask(prompt);
  }

  onEnter(event: Event): void {
    if (!(event instanceof KeyboardEvent)) return;
    if (event.shiftKey || this.loading()) return;
    event.preventDefault();
    this.submit();
  }

  retry(message: ChatMessage): void {
    if (this.loading() || !message.retryPrompt) return;
    this.requestAnswer(message.id, message.retryPrompt, message.retryHistory ?? []);
  }

  clearConversation(): void {
    if (this.loading()) return;
    this.messages.set([]);
    this.error.set(null);
  }

  formatMarkdown(value: string): string {
    const rendered = markdown.parse(value, { async: false });
    return this.sanitizer.sanitize(SecurityContext.HTML, rendered) ?? '';
  }

  private ask(prompt: string): void {
    const history = this.completedHistory();
    const userMessage: ChatMessage = {
      id: this.nextId++,
      role: 'USER',
      content: prompt,
      state: 'READY',
    };
    const answerId = this.nextId++;
    const answer: ChatMessage = {
      id: answerId,
      role: 'ASSISTANT',
      content: '',
      state: 'PENDING',
      retryPrompt: prompt,
      retryHistory: history,
    };
    this.messages.update((messages) => [...messages, userMessage, answer]);
    this.requestAnswer(answerId, prompt, history);
    this.scrollToLatest();
  }

  private requestAnswer(answerId: number, prompt: string, history: AiChatHistoryMessage[]): void {
    this.loading.set(true);
    this.error.set(null);
    this.messages.update((messages) =>
      messages.map((message) =>
        message.id === answerId ? { ...message, content: '', state: 'PENDING' } : message,
      ),
    );
    this.api
      .chat(prompt, history)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (content) => {
          this.messages.update((messages) =>
            messages.map((message) =>
              message.id === answerId ? { ...message, content, state: 'READY' } : message,
            ),
          );
          this.scrollToLatest();
        },
        error: (error: unknown) => {
          const message =
            error instanceof TimeoutError
              ? 'LifeSync AI is taking longer than expected. Please retry.'
              : readApiError(error, 'LifeSync AI could not respond. Please retry.');
          this.error.set(message);
          this.messages.update((messages) =>
            messages.map((item) =>
              item.id === answerId
                ? { ...item, content: message, state: 'ERROR', retryPrompt: prompt }
                : item,
            ),
          );
          this.scrollToLatest();
        },
      });
  }

  private completedHistory(): AiChatHistoryMessage[] {
    const history: AiChatHistoryMessage[] = [];
    const messages = this.messages();
    for (let index = 0; index < messages.length - 1; index += 2) {
      const user = messages[index];
      const assistant = messages[index + 1];
      if (user.role === 'USER' && assistant.role === 'ASSISTANT' && assistant.state === 'READY') {
        history.push(
          { role: 'USER', content: user.content },
          { role: 'ASSISTANT', content: assistant.content },
        );
      }
    }
    return history.slice(-12);
  }

  private scrollToLatest(): void {
    setTimeout(() => {
      const element = this.conversation?.nativeElement;
      if (element) element.scrollTop = element.scrollHeight;
    });
  }
}

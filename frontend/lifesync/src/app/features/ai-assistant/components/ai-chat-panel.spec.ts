import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { API_BASE_URL } from '../../../core/api.config';
import { AI_CHAT_TIMEOUT_MS } from '../data/ai-assistant-api.service';
import { AiChatPanel } from './ai-chat-panel';

describe('AiChatPanel', () => {
  let fixture: ComponentFixture<AiChatPanel>;
  let component: AiChatPanel;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    fixture = TestBed.createComponent(AiChatPanel);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify({ ignoreCancelled: true });
    vi.useRealTimers();
  });

  it('validates a nonblank message and renders a confirmed AI response', () => {
    component.form.controls.message.setValue('   ');
    component.submit();
    expect(component.messages()).toHaveLength(0);

    component.form.controls.message.setValue('  Help me focus.  ');
    component.submit();
    const request = http.expectOne(`${API_BASE_URL}/api/ai/chat`);
    expect(request.request.body).toEqual({ message: 'Help me focus.', history: [] });
    request.flush({ success: true, message: 'Generated', data: '## Start small\n- Pick one task' });

    expect(component.messages().map((message) => message.state)).toEqual(['READY', 'READY']);
    expect(component.loading()).toBe(false);
    expect(component.messages()[1].content).toContain('Pick one task');
  });

  it('renders rich Markdown and sanitizes raw HTML', () => {
    const formatted = component.formatMarkdown(
      [
        '## Today',
        '',
        '**HIGH** priority, *gentle* pace, and `one-task` focus.',
        '',
        '- First step',
        '- **2026-07-14**',
        '',
        '1. Start',
        '2. Review',
        '',
        '| Task | Status |',
        '| --- | --- |',
        '| Plan | PENDING |',
        '',
        '```ts',
        'const ready = true;',
        '```',
        '',
        '<script>alert(1)</script><img src=x onerror=alert(1)>',
      ].join('\n'),
    );

    expect(formatted).toContain('<h2>');
    expect(formatted).toContain('<strong>');
    expect(formatted).toContain('<em>gentle</em>');
    expect(formatted).toContain('<ul>');
    expect(formatted).toContain('<ol>');
    expect(formatted).toContain('<table>');
    expect(formatted).toContain('ai-markdown-badge--priority');
    expect(formatted).toContain('ai-markdown-badge--date');
    expect(formatted).toContain('<pre>');
    expect(formatted).toContain('language-ts');
    expect(formatted).toContain('&lt;img');
    expect(formatted).not.toContain('<script>');
    expect(formatted).not.toContain('<img');
  });

  it('renders Markdown structure in the assistant message', () => {
    component.askSuggestion('Show me a quick plan.');
    http.expectOne(`${API_BASE_URL}/api/ai/chat`).flush({
      success: true,
      message: 'Generated',
      data: '## A gentle plan\n\n| Step | Status |\n| --- | --- |\n| Begin | PENDING |',
    });
    fixture.detectChanges();

    const assistant = fixture.nativeElement.querySelector('.ai-markdown') as HTMLElement;
    expect(assistant.querySelector('h2')?.textContent).toContain('A gentle plan');
    expect(assistant.querySelector('table')).not.toBeNull();
    expect(assistant.querySelector('.ai-markdown-badge--status')).not.toBeNull();
  });

  it('shows retry after a backend failure and retries the same prompt', () => {
    component.askSuggestion('What should I focus on?');
    http
      .expectOne(`${API_BASE_URL}/api/ai/chat`)
      .flush({ message: 'AI temporarily unavailable' }, { status: 503, statusText: 'Unavailable' });
    expect(component.loading()).toBe(false);
    const answer = component.messages()[1];
    expect(answer.state).toBe('ERROR');

    component.retry(answer);
    const retry = http.expectOne(`${API_BASE_URL}/api/ai/chat`);
    expect(retry.request.body).toEqual({ message: 'What should I focus on?', history: [] });
    retry.flush({ success: true, message: 'Generated', data: 'Try one small step.' });
    expect(component.messages()[1]).toMatchObject({
      state: 'READY',
      content: 'Try one small step.',
    });
  });

  it('times out with a friendly retry message and resets the loading state', () => {
    vi.useFakeTimers();
    component.askSuggestion('What should I focus on?');
    const request = http.expectOne(`${API_BASE_URL}/api/ai/chat`);

    vi.advanceTimersByTime(AI_CHAT_TIMEOUT_MS);

    expect(component.loading()).toBe(false);
    expect(component.error()).toBe('LifeSync AI is taking longer than expected. Please retry.');
    expect(component.messages()[1].state).toBe('ERROR');
    expect(request.cancelled).toBe(true);
  });

  it('cancels the request and resets loading when the panel is destroyed', () => {
    component.askSuggestion('What should I focus on?');
    const request = http.expectOne(`${API_BASE_URL}/api/ai/chat`);

    fixture.destroy();

    expect(request.cancelled).toBe(true);
    expect(component.loading()).toBe(false);
  });

  it('does not submit a second chat request while one is active', () => {
    component.askSuggestion('First question');
    component.askSuggestion('Second question');

    const request = http.expectOne(`${API_BASE_URL}/api/ai/chat`);
    expect(request.request.body.message).toBe('First question');
    http.expectNone(`${API_BASE_URL}/api/ai/chat`);
    request.flush({ success: true, message: 'Generated', data: 'Answer.' });
  });

  it('sends successful in-session turns as context for the next message', () => {
    component.askSuggestion('I have a busy morning.');
    http
      .expectOne(`${API_BASE_URL}/api/ai/chat`)
      .flush({ success: true, message: 'Generated', data: 'Let’s choose one priority.' });

    component.askSuggestion('Can you help me choose it?');
    const request = http.expectOne(`${API_BASE_URL}/api/ai/chat`);
    expect(request.request.body).toEqual({
      message: 'Can you help me choose it?',
      history: [
        { role: 'USER', content: 'I have a busy morning.' },
        { role: 'ASSISTANT', content: 'Let’s choose one priority.' },
      ],
    });
    request.flush({
      success: true,
      message: 'Generated',
      data: 'Start with the nearest deadline.',
    });
  });
});

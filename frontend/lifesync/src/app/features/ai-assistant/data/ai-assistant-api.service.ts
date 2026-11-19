import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map, timeout } from 'rxjs';
import { API_BASE_URL } from '../../../core/api.config';
import {
  AiChatRequest,
  AiDailyBriefing,
  ApiEnvelope,
  DailyPlanRequest,
  DailyPlanResponse,
} from './ai-assistant.models';

export const AI_CHAT_TIMEOUT_MS = 68_000;

@Injectable({ providedIn: 'root' })
export class AiAssistantApiService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE_URL}/api/ai`;

  chat(message: string, history: AiChatRequest['history'] = []): Observable<string> {
    const request: AiChatRequest = { message, history: history.slice(-12) };
    return this.http.post<ApiEnvelope<string>>(`${this.url}/chat`, request).pipe(
      timeout({ first: AI_CHAT_TIMEOUT_MS }),
      map((response) => this.unwrap(response)),
    );
  }

  dailyBriefing(): Observable<AiDailyBriefing> {
    return this.http.get<AiDailyBriefing>(`${this.url}/daily-briefing`);
  }

  createDailyPlan(request: DailyPlanRequest): Observable<DailyPlanResponse> {
    return this.http
      .post<ApiEnvelope<DailyPlanResponse>>(`${this.url}/daily-plan`, request)
      .pipe(map((response) => this.unwrap(response)));
  }

  private unwrap<T>(response: ApiEnvelope<T>): T {
    if (!response?.success || response.data === null || response.data === undefined) {
      throw new Error(response?.message || 'The AI service returned an empty response.');
    }
    return response.data;
  }
}

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/api.config';
import {
  HabitCalendarResponse,
  HabitCompletionRequest,
  HabitLogRequest,
  HabitLogResponse,
  HabitStreakResponse,
} from './habit.models';

@Injectable({ providedIn: 'root' })
export class HabitLogApiService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE_URL}/api/habits`;

  create(habitId: number, request: HabitLogRequest): Observable<HabitLogResponse> {
    return this.http.post<HabitLogResponse>(`${this.url}/${habitId}/logs`, request);
  }

  list(habitId: number): Observable<HabitLogResponse[]> {
    return this.http.get<HabitLogResponse[]>(`${this.url}/${habitId}/logs`);
  }

  complete(habitId: number, request: HabitCompletionRequest): Observable<HabitLogResponse> {
    return this.http.patch<HabitLogResponse>(`${this.url}/${habitId}/complete`, request);
  }

  streak(habitId: number): Observable<HabitStreakResponse> {
    return this.http.get<HabitStreakResponse>(`${this.url}/${habitId}/streak`);
  }

  calendar(habitId: number, year: number, month: number): Observable<HabitCalendarResponse> {
    const params = new HttpParams().set('year', year).set('month', month);
    return this.http.get<HabitCalendarResponse>(`${this.url}/${habitId}/calendar`, { params });
  }
}

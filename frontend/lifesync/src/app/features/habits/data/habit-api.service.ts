import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/api.config';
import { HabitRequest, HabitResponse, HabitStatisticsResponse } from './habit.models';

@Injectable({ providedIn: 'root' })
export class HabitApiService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE_URL}/api/habits`;

  list(): Observable<HabitResponse[]> {
    return this.http.get<HabitResponse[]>(this.url);
  }

  get(id: number): Observable<HabitResponse> {
    return this.http.get<HabitResponse>(`${this.url}/${id}`);
  }

  create(request: HabitRequest): Observable<HabitResponse> {
    return this.http.post<HabitResponse>(this.url, request);
  }

  update(id: number, request: HabitRequest): Observable<HabitResponse> {
    return this.http.put<HabitResponse>(`${this.url}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  deactivate(id: number): Observable<HabitResponse> {
    return this.http.patch<HabitResponse>(`${this.url}/${id}/deactivate`, null);
  }

  statistics(): Observable<HabitStatisticsResponse> {
    return this.http.get<HabitStatisticsResponse>(`${this.url}/statistics`);
  }
}

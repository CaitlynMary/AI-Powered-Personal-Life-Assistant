import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/api.config';
import { GoalRequest, GoalResponse, GoalStatus } from './goal.models';

@Injectable({ providedIn: 'root' })
export class GoalApiService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE_URL}/api/goals`;

  list(): Observable<GoalResponse[]> {
    return this.http.get<GoalResponse[]>(this.url);
  }

  get(id: number): Observable<GoalResponse> {
    return this.http.get<GoalResponse>(`${this.url}/${id}`);
  }

  create(request: GoalRequest): Observable<GoalResponse> {
    return this.http.post<GoalResponse>(this.url, request);
  }

  update(id: number, request: GoalRequest): Observable<GoalResponse> {
    return this.http.put<GoalResponse>(`${this.url}/${id}`, request);
  }

  updateStatus(id: number, status: GoalStatus): Observable<GoalResponse> {
    return this.http.patch<GoalResponse>(`${this.url}/${id}/status`, { status });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}

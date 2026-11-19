import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/api.config';
import { MilestoneRequest, MilestoneResponse } from './goal.models';

@Injectable({ providedIn: 'root' })
export class MilestoneApiService {
  private readonly http = inject(HttpClient);
  private readonly goalsUrl = `${API_BASE_URL}/api/goals`;
  private readonly milestonesUrl = `${API_BASE_URL}/api/milestones`;

  listForGoal(goalId: number): Observable<MilestoneResponse[]> {
    return this.http.get<MilestoneResponse[]>(`${this.goalsUrl}/${goalId}/milestones`);
  }

  get(id: number): Observable<MilestoneResponse> {
    return this.http.get<MilestoneResponse>(`${this.milestonesUrl}/${id}`);
  }

  create(goalId: number, request: MilestoneRequest): Observable<MilestoneResponse> {
    return this.http.post<MilestoneResponse>(`${this.goalsUrl}/${goalId}/milestones`, request);
  }

  update(id: number, request: MilestoneRequest): Observable<MilestoneResponse> {
    return this.http.put<MilestoneResponse>(`${this.milestonesUrl}/${id}`, request);
  }

  complete(id: number): Observable<MilestoneResponse> {
    return this.http.patch<MilestoneResponse>(`${this.milestonesUrl}/${id}/complete`, null);
  }

  updateStatus(id: number, status: MilestoneResponse['status']): Observable<MilestoneResponse> {
    return this.http.patch<MilestoneResponse>(`${this.milestonesUrl}/${id}/status`, { status });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.milestonesUrl}/${id}`);
  }
}

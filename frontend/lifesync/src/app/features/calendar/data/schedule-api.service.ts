import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map, of, switchMap } from 'rxjs';
import { API_BASE_URL } from '../../../core/api.config';
import {
  PageResponse,
  ScheduleConflictRequest,
  ScheduleConflictResponse,
  DailyScheduleApprovalRequest,
  ScheduleSlotRequest,
  ScheduleSlotResponse,
} from './calendar.models';

const PAGE_SIZE = 100;

@Injectable({ providedIn: 'root' })
export class ScheduleApiService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE_URL}/api/schedule`;

  listPage(page = 0): Observable<PageResponse<ScheduleSlotResponse>> {
    const params = new HttpParams().set('page', page).set('size', PAGE_SIZE);
    return this.http.get<PageResponse<ScheduleSlotResponse>>(this.url, { params });
  }

  listAll(): Observable<ScheduleSlotResponse[]> {
    return this.listPage().pipe(
      switchMap((first) => {
        const remaining = Array.from({ length: Math.max(0, first.totalPages - 1) }, (_, index) =>
          this.listPage(index + 1),
        );
        return remaining.length
          ? forkJoin(remaining).pipe(
              map((pages) => [...first.content, ...pages.flatMap((page) => page.content)]),
            )
          : of(first.content);
      }),
    );
  }

  daily(date: string): Observable<ScheduleSlotResponse[]> {
    return this.http.get<ScheduleSlotResponse[]>(`${this.url}/daily`, {
      params: new HttpParams().set('date', date),
    });
  }

  weekly(startDate: string): Observable<ScheduleSlotResponse[]> {
    return this.http.get<ScheduleSlotResponse[]>(`${this.url}/weekly`, {
      params: new HttpParams().set('startDate', startDate),
    });
  }

  get(id: number): Observable<ScheduleSlotResponse> {
    return this.http.get<ScheduleSlotResponse>(`${this.url}/${id}`);
  }

  create(request: ScheduleSlotRequest): Observable<ScheduleSlotResponse> {
    return this.http.post<ScheduleSlotResponse>(this.url, request);
  }

  update(id: number, request: ScheduleSlotRequest): Observable<ScheduleSlotResponse> {
    return this.http.put<ScheduleSlotResponse>(`${this.url}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  detectConflicts(request: ScheduleConflictRequest): Observable<ScheduleConflictResponse> {
    return this.http.post<ScheduleConflictResponse>(`${this.url}/conflicts`, request);
  }

  approveDailyPlan(request: DailyScheduleApprovalRequest): Observable<ScheduleSlotResponse[]> {
    return this.http.post<ScheduleSlotResponse[]>(`${this.url}/approve-daily-plan`, request);
  }
}

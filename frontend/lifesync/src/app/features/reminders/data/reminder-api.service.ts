import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, expand, of, reduce } from 'rxjs';
import { API_BASE_URL } from '../../../core/api.config';
import { ReminderPageResponse, ReminderRequest, ReminderResponse } from './reminder.models';

const PAGE_SIZE = 100;

@Injectable({ providedIn: 'root' })
export class ReminderApiService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE_URL}/api/reminders`;

  list(page = 0, size = 20): Observable<ReminderPageResponse> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ReminderPageResponse>(this.url, { params });
  }

  listAll(): Observable<ReminderResponse[]> {
    return this.list(0, PAGE_SIZE).pipe(
      expand((page) => (page.last ? of() : this.list(page.page + 1, PAGE_SIZE))),
      reduce((reminders, page) => [...reminders, ...page.content], [] as ReminderResponse[]),
    );
  }

  upcoming(page = 0, size = 20): Observable<ReminderPageResponse> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ReminderPageResponse>(`${this.url}/upcoming`, { params });
  }

  get(id: number): Observable<ReminderResponse> {
    return this.http.get<ReminderResponse>(`${this.url}/${id}`);
  }

  create(request: ReminderRequest): Observable<ReminderResponse> {
    return this.http.post<ReminderResponse>(this.url, request);
  }

  update(id: number, request: ReminderRequest): Observable<ReminderResponse> {
    return this.http.put<ReminderResponse>(`${this.url}/${id}`, request);
  }

  cancel(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}

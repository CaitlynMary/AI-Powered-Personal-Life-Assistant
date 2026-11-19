import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/api.config';
import { CalendarEventRequest, CalendarEventResponse, CalendarEventType } from './calendar.models';

@Injectable({ providedIn: 'root' })
export class CalendarEventApiService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE_URL}/api/calendar/events`;

  list(
    startDateTime?: string,
    endDateTime?: string,
    eventType?: CalendarEventType | '',
  ): Observable<CalendarEventResponse[]> {
    let params = new HttpParams();
    if (startDateTime && endDateTime) {
      params = params.set('startDateTime', startDateTime).set('endDateTime', endDateTime);
    }
    if (eventType) params = params.set('eventType', eventType);
    return this.http.get<CalendarEventResponse[]>(this.url, { params });
  }

  day(date: string): Observable<CalendarEventResponse[]> {
    return this.http.get<CalendarEventResponse[]>(`${this.url}/day`, {
      params: new HttpParams().set('date', date),
    });
  }

  week(startDate: string): Observable<CalendarEventResponse[]> {
    return this.http.get<CalendarEventResponse[]>(`${this.url}/week`, {
      params: new HttpParams().set('startDate', startDate),
    });
  }

  month(year: number, month: number): Observable<CalendarEventResponse[]> {
    return this.http.get<CalendarEventResponse[]>(`${this.url}/month`, {
      params: new HttpParams().set('year', year).set('month', month),
    });
  }

  get(id: number): Observable<CalendarEventResponse> {
    return this.http.get<CalendarEventResponse>(`${this.url}/${id}`);
  }

  create(request: CalendarEventRequest): Observable<CalendarEventResponse> {
    return this.http.post<CalendarEventResponse>(this.url, request);
  }

  update(id: number, request: CalendarEventRequest): Observable<CalendarEventResponse> {
    return this.http.put<CalendarEventResponse>(`${this.url}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}

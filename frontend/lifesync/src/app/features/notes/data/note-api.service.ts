import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, expand, of, reduce } from 'rxjs';
import { API_BASE_URL } from '../../../core/api.config';
import {
  NotePageResponse,
  NoteQuery,
  NoteRequest,
  NoteResponse,
  NoteStatisticsResponse,
} from './note.models';

const PAGE_SIZE = 100;

@Injectable({ providedIn: 'root' })
export class NoteApiService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE_URL}/api/notes`;

  list(query: NoteQuery = {}): Observable<NotePageResponse> {
    let params = new HttpParams()
      .set('page', query.page ?? 0)
      .set('size', query.size ?? 20)
      .set('sortBy', query.sortBy ?? 'updatedAt')
      .set('direction', query.direction ?? 'DESC');
    if (query.search?.trim()) params = params.set('search', query.search.trim());
    return this.http.get<NotePageResponse>(this.url, { params });
  }

  listAll(query: Omit<NoteQuery, 'page' | 'size'> = {}): Observable<NoteResponse[]> {
    return this.list({ ...query, page: 0, size: PAGE_SIZE }).pipe(
      expand((page) =>
        page.last ? of() : this.list({ ...query, page: page.page + 1, size: PAGE_SIZE }),
      ),
      reduce((notes, page) => [...notes, ...page.content], [] as NoteResponse[]),
    );
  }

  journal(date: string): Observable<NoteResponse[]> {
    return this.http.get<NoteResponse[]>(`${this.url}/journal`, {
      params: new HttpParams().set('date', date),
    });
  }

  statistics(): Observable<NoteStatisticsResponse> {
    return this.http.get<NoteStatisticsResponse>(`${this.url}/statistics`);
  }

  get(id: number): Observable<NoteResponse> {
    return this.http.get<NoteResponse>(`${this.url}/${id}`);
  }

  create(request: NoteRequest): Observable<NoteResponse> {
    return this.http.post<NoteResponse>(this.url, request);
  }

  update(id: number, request: NoteRequest): Observable<NoteResponse> {
    return this.http.put<NoteResponse>(`${this.url}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  togglePin(id: number): Observable<NoteResponse> {
    return this.http.patch<NoteResponse>(`${this.url}/${id}/pin`, null);
  }
}

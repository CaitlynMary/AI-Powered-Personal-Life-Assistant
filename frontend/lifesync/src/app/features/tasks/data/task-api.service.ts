import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map, of, switchMap } from 'rxjs';
import { API_BASE_URL } from '../../../core/api.config';
import { TaskPageResponse, TaskQuery, TaskRequest, TaskResponse, TaskUpdateStatus } from './task.models';

@Injectable({ providedIn: 'root' })
export class TaskApiService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE_URL}/api/tasks`;

  list(query: TaskQuery = {}): Observable<TaskPageResponse> {
    let params = new HttpParams()
      .set('page', query.page ?? 0)
      .set('size', query.size ?? 20)
      .set('sortBy', query.sortBy ?? 'createdAt')
      .set('direction', query.direction ?? 'DESC');

    if (query.status) params = params.set('status', query.status);
    if (query.priority) params = params.set('priority', query.priority);
    if (query.categoryId) params = params.set('categoryId', query.categoryId);
    if (query.search?.trim()) params = params.set('search', query.search.trim());
    if (query.dueFrom) params = params.set('dueFrom', query.dueFrom);
    if (query.dueTo) params = params.set('dueTo', query.dueTo);

    return this.http.get<TaskPageResponse>(this.url, { params });
  }

  listAll(query: Omit<TaskQuery, 'page' | 'size'> = {}): Observable<TaskResponse[]> {
    const size = 100;
    return this.list({ ...query, page: 0, size }).pipe(
      switchMap((first) => {
        const remaining = Array.from({ length: Math.max(0, first.totalPages - 1) }, (_, index) =>
          this.list({ ...query, page: index + 1, size }),
        );
        return remaining.length
          ? forkJoin(remaining).pipe(
              map((pages) => [...first.content, ...pages.flatMap((page) => page.content)]),
            )
          : of(first.content);
      }),
    );
  }

  get(id: number): Observable<TaskResponse> {
    return this.http.get<TaskResponse>(`${this.url}/${id}`);
  }

  create(request: TaskRequest): Observable<TaskResponse> {
    return this.http.post<TaskResponse>(this.url, request);
  }

  update(id: number, request: TaskRequest): Observable<TaskResponse> {
    return this.http.put<TaskResponse>(`${this.url}/${id}`, request);
  }

  setStatus(id: number, status: TaskUpdateStatus): Observable<TaskResponse> {
    return this.http.patch<TaskResponse>(`${this.url}/${id}/status`, { status });
  }

  complete(id: number): Observable<TaskResponse> {
    return this.http.patch<TaskResponse>(`${this.url}/${id}/complete`, null);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}

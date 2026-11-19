import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/api.config';
import { UserSettings } from './user-settings.models';

@Injectable({ providedIn: 'root' })
export class UserSettingsApiService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${API_BASE_URL}/api/users/me/settings`;

  get(): Observable<UserSettings> {
    return this.http.get<UserSettings>(this.endpoint);
  }

  update(settings: UserSettings): Observable<UserSettings> {
    return this.http.put<UserSettings>(this.endpoint, settings);
  }
}

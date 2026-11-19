import { Injectable, signal } from '@angular/core';
import { DEFAULT_USER_SETTINGS, UserSettings } from './user-settings.models';

@Injectable({ providedIn: 'root' })
export class UserSettingsStateService {
  readonly value = signal<UserSettings>(DEFAULT_USER_SETTINGS);
}

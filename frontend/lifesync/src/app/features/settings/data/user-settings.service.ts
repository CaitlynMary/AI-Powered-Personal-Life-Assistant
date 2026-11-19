import { Injectable, effect, inject, signal } from '@angular/core';
import { firstValueFrom, Observable, catchError, finalize, of, tap } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { readApiError } from '../../../core/auth/auth-error';
import { UserSettingsApiService } from './user-settings-api.service';
import { UserSettingsStateService } from './user-settings-state.service';
import { DEFAULT_USER_SETTINGS, UserSettings } from './user-settings.models';

@Injectable({ providedIn: 'root' })
export class UserSettingsService {
  private readonly auth = inject(AuthService);
  private readonly api = inject(UserSettingsApiService);
  private readonly state = inject(UserSettingsStateService);
  private currentUserId: number | null = null;
  private loadedUserId: number | null = null;
  private inFlightUserId: number | null = null;
  private inFlight: Promise<void> | null = null;

  readonly value = this.state.value;
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  constructor() {
    effect(() => this.activate(this.auth.currentUserProfile()?.id ?? null));
  }

  loadForCurrentUser(): Promise<void> {
    const userId = this.auth.currentUserProfile()?.id ?? null;
    this.activate(userId);
    return userId === null ? Promise.resolve() : this.fetchForUser(userId);
  }

  save(settings: UserSettings): Observable<UserSettings> {
    const userId = this.currentUserId;
    if (userId === null) {
      throw new Error('You must be signed in to save settings.');
    }
    return this.api.update(settings).pipe(
      tap((saved) => {
        if (this.currentUserId === userId) {
          this.state.value.set(saved);
          this.loadedUserId = userId;
          this.error.set(null);
        }
      }),
    );
  }

  private activate(userId: number | null): void {
    if (userId === this.currentUserId) return;
    this.currentUserId = userId;
    this.loadedUserId = null;
    this.state.value.set(DEFAULT_USER_SETTINGS);
    this.error.set(null);
    this.loading.set(false);
    if (userId !== null) void this.fetchForUser(userId);
  }

  private fetchForUser(userId: number): Promise<void> {
    if (this.loadedUserId === userId) return Promise.resolve();
    if (this.inFlightUserId === userId && this.inFlight) return this.inFlight;

    this.loading.set(true);
    this.error.set(null);
    this.inFlightUserId = userId;

    let request: Promise<void>;
    request = firstValueFrom(
      this.api.get().pipe(
        tap((settings) => {
          if (this.currentUserId === userId) {
            this.state.value.set(settings);
            this.loadedUserId = userId;
          }
        }),
        catchError((error: unknown) => {
          if (this.currentUserId === userId) {
            this.error.set(readApiError(error, 'Could not load your saved preferences.'));
          }
          return of(null);
        }),
        finalize(() => {
          if (this.currentUserId === userId) this.loading.set(false);
        }),
      ),
    )
      .then(() => undefined)
      .finally(() => {
        if (this.inFlight === request) {
          this.inFlight = null;
          this.inFlightUserId = null;
        }
      });
    this.inFlight = request;
    return request;
  }
}

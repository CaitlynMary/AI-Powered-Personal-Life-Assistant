import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import {
  Observable,
  catchError,
  finalize,
  firstValueFrom,
  map,
  of,
  switchMap,
  tap,
} from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { SKIP_AUTH } from './auth.context';
import { readApiError } from './auth-error';
import {
  LoginRequest,
  LoginResponse,
  ChangePasswordRequest,
  RegisterRequest,
  TokenResponse,
  UpdateUserRequest,
  UserProfile,
} from './auth.models';

const REFRESH_KEY = 'lifesync-refresh-token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  /** Access token lives in memory only. It is restored from the refresh token on startup. */
  private readonly accessToken = signal<string | null>(null);
  private refreshInFlight: Promise<string | null> | null = null;
  private sessionReady: Promise<void> | null = null;
  private sessionVersion = 0;
  private profileRequestId = 0;
  private readonly profileState = signal<UserProfile | null>(null);

  readonly isAuthenticated = computed(() => this.accessToken() !== null);
  readonly currentUserProfile = this.profileState.asReadonly();
  readonly profileLoading = signal(false);
  readonly profileError = signal<string | null>(null);

  /**
   * Runs once before protected routes are evaluated.
   * A stored refresh token is exchanged for a new access token and profile.
   */
  restoreSession(): Promise<void> {
    this.sessionReady ??= this.restore();
    return this.sessionReady;
  }

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${API_BASE_URL}/api/auth/login`, request, {
        context: this.publicContext(),
      })
      .pipe(
        tap((tokens) => {
          this.sessionVersion += 1;
          this.profileState.set(null);
          this.profileError.set(null);
          this.saveTokens(tokens);
        }),
        switchMap((tokens) => this.loadProfile().pipe(map(() => tokens))),
      );
  }

  register(request: RegisterRequest): Observable<UserProfile> {
    return this.http.post<UserProfile>(`${API_BASE_URL}/api/users/register`, request, {
      context: this.publicContext(),
    });
  }

  currentUser(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${API_BASE_URL}/api/users/me`);
  }

  updateMyProfile(request: UpdateUserRequest): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${API_BASE_URL}/api/users/me`, request).pipe(
      tap((profile) => this.profileState.set(profile)),
    );
  }

  changeMyPassword(request: ChangePasswordRequest): Observable<void> {
    return this.http.patch<void>(`${API_BASE_URL}/api/users/me/password`, request);
  }

  /**
   * One shared refresh for every caller. The refresh call itself is marked
   * public so a 401 cannot start another refresh.
   */
  refreshAccessToken(): Promise<string | null> {
    if (this.refreshInFlight) {
      return this.refreshInFlight;
    }

    const refreshToken = sessionStorage.getItem(REFRESH_KEY);
    if (!refreshToken) {
      this.clearSession();
      return Promise.resolve(null);
    }

    this.refreshInFlight = firstValueFrom(
      this.http.post<TokenResponse>(
        `${API_BASE_URL}/api/auth/refresh`,
        { refreshToken },
        { context: this.publicContext() },
      ),
    )
      .then((tokens) => {
        this.saveTokens(tokens);
        return tokens.accessToken;
      })
      .catch(() => {
        this.clearSession();
        return null;
      })
      .finally(() => {
        this.refreshInFlight = null;
      });

    return this.refreshInFlight;
  }

  /** Local session is cleared even when the logout API call fails. */
  logout(): Observable<void> {
    const refreshToken = sessionStorage.getItem(REFRESH_KEY);
    const finish = () => {
      this.clearSession();
      void this.router.navigate(['/login']);
    };

    if (!refreshToken) {
      finish();
      return of(undefined);
    }

    return this.http
      .post<void>(
        `${API_BASE_URL}/api/auth/logout`,
        { refreshToken },
        { context: this.publicContext() },
      )
      .pipe(
        catchError(() => of(undefined)),
        finalize(finish),
        map(() => undefined),
      );
  }

  token(): string | null {
    return this.accessToken();
  }

  clearSession(): void {
    this.sessionVersion += 1;
    this.profileRequestId += 1;
    this.accessToken.set(null);
    this.profileState.set(null);
    this.profileLoading.set(false);
    this.profileError.set(null);
    sessionStorage.removeItem(REFRESH_KEY);
  }

  private async restore(): Promise<void> {
    if (sessionStorage.getItem(REFRESH_KEY)) {
      const token = await this.refreshAccessToken();
      if (token) {
        await firstValueFrom(this.loadProfile());
      }
    }
  }

  private loadProfile(): Observable<UserProfile | null> {
    const requestId = ++this.profileRequestId;
    const sessionVersion = this.sessionVersion;
    this.profileLoading.set(true);
    this.profileError.set(null);

    return this.currentUser().pipe(
      tap((profile) => {
        if (this.isCurrentProfileRequest(requestId, sessionVersion)) {
          this.profileState.set(profile);
        }
      }),
      catchError((error: unknown) => {
        if (this.isCurrentProfileRequest(requestId, sessionVersion)) {
          this.profileState.set(null);
          this.profileError.set(readApiError(error, 'Could not load your profile.'));
        }
        return of(null);
      }),
      finalize(() => {
        if (requestId === this.profileRequestId) {
          this.profileLoading.set(false);
        }
      }),
    );
  }

  private isCurrentProfileRequest(requestId: number, sessionVersion: number): boolean {
    return (
      requestId === this.profileRequestId &&
      sessionVersion === this.sessionVersion &&
      this.isAuthenticated()
    );
  }

  private saveTokens(tokens: TokenResponse): void {
    this.accessToken.set(tokens.accessToken);
    sessionStorage.setItem(REFRESH_KEY, tokens.refreshToken);
  }

  private publicContext(): HttpContext {
    return new HttpContext().set(SKIP_AUTH, true);
  }
}

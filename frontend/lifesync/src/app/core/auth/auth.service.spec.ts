import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { LoginResponse, TokenResponse, UserProfile } from './auth.models';

const loginResponse: LoginResponse = {
  message: 'Login successful',
  accessToken: 'access-token',
  refreshToken: 'refresh-token',
  tokenType: 'Bearer',
  expiresIn: 900,
};

const refreshedTokens: TokenResponse = {
  accessToken: 'restored-access-token',
  refreshToken: 'rotated-refresh-token',
  tokenType: 'Bearer',
  expiresIn: 900,
};

const profile: UserProfile = {
  id: 7,
  fullName: 'Alex Morgan',
  email: 'alex@example.com',
  phoneNumber: null,
  active: true,
  createdAt: '2026-11-11T12:00:00',
};

function nextTask(): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, 0));
}

@Component({ standalone: true, template: '' })
class LoginRoute {}

describe('AuthService current-user profile', () => {
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([{ path: 'login', component: LoginRoute }]),
      ],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  it('loads the profile after login and stores the authenticated user', async () => {
    const login = firstValueFrom(auth.login({ email: profile.email, password: 'password' }));

    http.expectOne(`${API_BASE_URL}/api/auth/login`).flush(loginResponse);
    const profileRequest = http.expectOne(`${API_BASE_URL}/api/users/me`);
    expect(profileRequest.request.method).toBe('GET');
    expect(profileRequest.request.headers.get('Authorization')).toBe('Bearer access-token');
    profileRequest.flush(profile);

    await login;

    expect(auth.currentUserProfile()).toEqual(profile);
    expect(auth.profileLoading()).toBe(false);
    expect(auth.profileError()).toBeNull();
  });

  it('loads the profile after restoring a session', async () => {
    sessionStorage.setItem('lifesync-refresh-token', 'previous-refresh-token');
    const restoration = auth.restoreSession();

    const refreshRequest = http.expectOne(`${API_BASE_URL}/api/auth/refresh`);
    expect(refreshRequest.request.method).toBe('POST');
    refreshRequest.flush(refreshedTokens);
    await nextTask();
    const profileRequest = http.expectOne(`${API_BASE_URL}/api/users/me`);
    expect(profileRequest.request.headers.get('Authorization')).toBe(
      'Bearer restored-access-token',
    );
    profileRequest.flush(profile);

    await restoration;

    expect(auth.isAuthenticated()).toBe(true);
    expect(auth.currentUserProfile()).toEqual(profile);
  });

  it('keeps the session usable and exposes an error when profile retrieval fails', async () => {
    const login = firstValueFrom(auth.login({ email: profile.email, password: 'password' }));

    http.expectOne(`${API_BASE_URL}/api/auth/login`).flush(loginResponse);
    http
      .expectOne(`${API_BASE_URL}/api/users/me`)
      .flush(
        { message: 'Profile service unavailable' },
        { status: 500, statusText: 'Server Error' },
      );

    await login;

    expect(auth.isAuthenticated()).toBe(true);
    expect(auth.currentUserProfile()).toBeNull();
    expect(auth.profileLoading()).toBe(false);
    expect(auth.profileError()).toBe('Profile service unavailable');
  });

  it('keeps an unauthenticated session without attempting profile retrieval', async () => {
    await auth.restoreSession();

    http.expectNone(`${API_BASE_URL}/api/users/me`);
    expect(auth.isAuthenticated()).toBe(false);
    expect(auth.currentUserProfile()).toBeNull();
    expect(auth.profileLoading()).toBe(false);
    expect(auth.profileError()).toBeNull();
  });

  it('clears the profile when logging out', async () => {
    const login = firstValueFrom(auth.login({ email: profile.email, password: 'password' }));
    http.expectOne(`${API_BASE_URL}/api/auth/login`).flush(loginResponse);
    http.expectOne(`${API_BASE_URL}/api/users/me`).flush(profile);
    await login;

    const logout = firstValueFrom(auth.logout());
    const logoutRequest = http.expectOne(`${API_BASE_URL}/api/auth/logout`);
    expect(logoutRequest.request.method).toBe('POST');
    logoutRequest.flush(null);
    await logout;

    expect(auth.isAuthenticated()).toBe(false);
    expect(auth.currentUserProfile()).toBeNull();
    expect(sessionStorage.getItem('lifesync-refresh-token')).toBeNull();
  });
});

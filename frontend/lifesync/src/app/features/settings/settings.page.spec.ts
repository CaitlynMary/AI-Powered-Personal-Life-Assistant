import { signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, ParamMap, Router } from '@angular/router';
import {
  LucideArrowRight,
  LucideBell,
  LucideBot,
  LucideCalendarDays,
  LucideLock,
  LucideSparkles,
  LucideSun,
  LucideUser,
  provideLucideIcons,
} from '@lucide/angular';
import { BehaviorSubject, of } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ThemeService } from '../../core/services/theme.service';
import { UserProfile } from '../../core/auth/auth.models';
import { UserSettingsService } from './data/user-settings.service';
import { DEFAULT_USER_SETTINGS, UserSettings } from './data/user-settings.models';
import { SettingsPage } from './settings.page';

const profile: UserProfile = {
  id: 32,
  fullName: 'Caitlyn Mary',
  email: 'caitlyn@example.test',
  phoneNumber: null,
  active: true,
  createdAt: '2026-01-01T10:00:00',
};

describe('SettingsPage', () => {
  let fixture: ComponentFixture<SettingsPage>;
  let component: SettingsPage;
  let auth: {
    currentUserProfile: WritableSignal<UserProfile | null>;
    updateMyProfile: ReturnType<typeof vi.fn>;
    changeMyPassword: ReturnType<typeof vi.fn>;
    logout: ReturnType<typeof vi.fn>;
  };
  let preferences: {
    value: WritableSignal<UserSettings>;
    loading: WritableSignal<boolean>;
    error: WritableSignal<string | null>;
    save: ReturnType<typeof vi.fn>;
    loadForCurrentUser: ReturnType<typeof vi.fn>;
  };
  let theme: { mode: WritableSignal<'light' | 'dark'>; set: ReturnType<typeof vi.fn> };
  let routeParams: BehaviorSubject<ParamMap>;
  let router: { navigate: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    routeParams = new BehaviorSubject(convertToParamMap({}));
    router = { navigate: vi.fn().mockResolvedValue(true) };
    auth = {
      currentUserProfile: signal(profile),
      updateMyProfile: vi.fn((request: { fullName: string; phoneNumber: string | null }) =>
        of({ ...profile, ...request }),
      ),
      changeMyPassword: vi.fn(() => of(undefined)),
      logout: vi.fn(() => of(undefined)),
    };
    preferences = {
      value: signal(DEFAULT_USER_SETTINGS),
      loading: signal(false),
      error: signal<string | null>(null),
      save: vi.fn((settings) => of(settings)),
      loadForCurrentUser: vi.fn(() => Promise.resolve()),
    };
    theme = { mode: signal<'light' | 'dark'>('light'), set: vi.fn() };

    TestBed.configureTestingModule({
      imports: [SettingsPage],
      providers: [
        { provide: ActivatedRoute, useValue: { queryParamMap: routeParams.asObservable() } },
        { provide: Router, useValue: router },
        provideLucideIcons(
          LucideArrowRight,
          LucideBell,
          LucideBot,
          LucideCalendarDays,
          LucideLock,
          LucideSparkles,
          LucideSun,
          LucideUser,
        ),
        { provide: AuthService, useValue: auth },
        { provide: UserSettingsService, useValue: preferences },
        { provide: ThemeService, useValue: theme },
      ],
    });
    fixture = TestBed.createComponent(SettingsPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('shows all seven settings sections and the signed-in profile identity', () => {
    expect(component.sectionCards).toHaveLength(7);
    expect(component.profileInitials()).toBe('CM');
    expect(fixture.nativeElement.textContent).toContain('My Profile');
    expect(fixture.nativeElement.textContent).toContain('About LifeSync');
  });

  it('navigates to each section and returns to the overview with Angular Router state', () => {
    component.sectionCards.forEach((card, index) => {
      component.openSection(card.id);
      expect(router.navigate).toHaveBeenNthCalledWith(
        index + 1,
        [],
        expect.objectContaining({
          queryParams: { section: card.id },
          queryParamsHandling: 'merge',
        }),
      );
    });

    component.backToOverview();
    expect(router.navigate).toHaveBeenLastCalledWith(
      [],
      expect.objectContaining({ queryParams: { section: null } }),
    );
  });

  it('saves editable profile fields without changing the authenticated account identity', () => {
    component.profileForm.setValue({ fullName: 'Taylor Example', phoneNumber: '' });

    component.saveProfile();

    expect(auth.updateMyProfile).toHaveBeenCalledWith({
      fullName: 'Taylor Example',
      phoneNumber: null,
    });
    expect(component.profileSuccess()).toBe('Your profile has been updated.');
  });

  it('saves settings through the user-scoped preferences API', () => {
    component.preferencesForm.patchValue({
      timeZoneId: 'Asia/Kolkata',
      aiResponseStyle: 'CONCISE',
      aiPersonalizationEnabled: false,
    });

    component.savePreferences();

    expect(preferences.save).toHaveBeenCalledWith(
      expect.objectContaining({
        timeZoneId: 'Asia/Kolkata',
        aiResponseStyle: 'CONCISE',
        aiPersonalizationEnabled: false,
      }),
    );
    expect(component.preferencesSuccess()).toBe('Your preferences have been saved.');
  });

  it('does not send an invalid profile', () => {
    component.profileForm.setValue({ fullName: '   ', phoneNumber: '' });

    component.saveProfile();

    expect(auth.updateMyProfile).not.toHaveBeenCalled();
  });
});

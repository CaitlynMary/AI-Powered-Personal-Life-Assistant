import { CommonModule } from '@angular/common';
import { Component, effect, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideDynamicIcon } from '@lucide/angular';
import { toSignal } from '@angular/core/rxjs-interop';
import { finalize, map } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { readApiError } from '../../core/auth/auth-error';
import { ThemeMode, ThemeService } from '../../core/services/theme.service';
import { UserSettingsService } from './data/user-settings.service';
import packageJson from '../../../../package.json';
import {
  AiResponseStyle,
  DateFormatPreference,
  TimeFormatPreference,
  UserSettings,
  WeekStartPreference,
} from './data/user-settings.models';

type SettingsSection =
  | 'profile'
  | 'appearance'
  | 'notifications'
  | 'regional'
  | 'security'
  | 'ai'
  | 'about';

const SETTINGS_SECTIONS: readonly SettingsSection[] = [
  'profile',
  'appearance',
  'notifications',
  'regional',
  'security',
  'ai',
  'about',
];

@Component({
  selector: 'app-settings-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LucideDynamicIcon],
  templateUrl: './settings.page.html',
})
export class SettingsPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly userSettings = inject(UserSettingsService);
  readonly theme = inject(ThemeService);

  readonly profile = this.auth.currentUserProfile;
  readonly activeSection = toSignal(
    this.route.queryParamMap.pipe(
      map((params) => {
        const section = params.get('section');
        return SETTINGS_SECTIONS.includes(section as SettingsSection)
          ? (section as SettingsSection)
          : null;
      }),
    ),
    { initialValue: null },
  );
  readonly preferencesLoading = this.userSettings.loading;
  readonly preferencesLoadError = this.userSettings.error;
  readonly profileSaving = signal(false);
  readonly profileSuccess = signal<string | null>(null);
  readonly profileError = signal<string | null>(null);
  readonly preferencesSaving = signal(false);
  readonly preferencesSuccess = signal<string | null>(null);
  readonly preferencesError = signal<string | null>(null);
  readonly passwordSaving = signal(false);
  readonly passwordSuccess = signal<string | null>(null);
  readonly passwordError = signal<string | null>(null);
  readonly loggingOut = signal(false);
  readonly logoutError = signal<string | null>(null);

  readonly sectionCards = [
    { id: 'profile', title: 'My Profile', description: 'Manage your name and contact details.', icon: 'user', accent: 'bg-powder-blue/45 text-plum' },
    { id: 'appearance', title: 'Appearance', description: 'Choose a light or dark workspace.', icon: 'sun', accent: 'bg-soft-pink/25 text-plum' },
    { id: 'notifications', title: 'Notifications', description: 'Choose which in-app updates you receive.', icon: 'bell', accent: 'bg-aqua/40 text-plum' },
    { id: 'regional', title: 'Regional Settings', description: 'Set your time zone and calendar preferences.', icon: 'calendar-days', accent: 'bg-lime/25 text-plum' },
    { id: 'security', title: 'Security & Privacy', description: 'Update your password or sign out.', icon: 'lock', accent: 'bg-purple/15 text-plum' },
    { id: 'ai', title: 'AI Preferences', description: 'Personalize LifeSync AI responses.', icon: 'bot', accent: 'bg-powder-blue/55 text-plum' },
    { id: 'about', title: 'About LifeSync', description: 'Learn about the app and its technology.', icon: 'sparkles', accent: 'bg-mint/45 text-plum' },
  ] as const;

  readonly timeZones = [
    { value: 'DEVICE', label: 'Use device time zone' },
    { value: 'UTC', label: 'UTC' },
    ...Intl.supportedValuesOf('timeZone')
      .filter((timeZone) => timeZone !== 'UTC')
      .map((timeZone) => ({ value: timeZone, label: timeZone.replaceAll('_', ' ') })),
  ];

  readonly dateFormats: { value: DateFormatPreference; label: string }[] = [
    { value: 'LOCALE', label: 'Use device locale' },
    { value: 'MM_DD_YYYY', label: 'MM/DD/YYYY' },
    { value: 'DD_MM_YYYY', label: 'DD/MM/YYYY' },
    { value: 'YYYY_MM_DD', label: 'YYYY-MM-DD' },
  ];

  readonly timeFormats: { value: TimeFormatPreference; label: string }[] = [
    { value: 'LOCALE', label: 'Use device locale' },
    { value: 'H12', label: '12-hour clock' },
    { value: 'H24', label: '24-hour clock' },
  ];

  readonly aiResponseStyles: { value: AiResponseStyle; label: string; description: string }[] = [
    { value: 'CONCISE', label: 'Concise', description: 'Short, direct answers and next steps.' },
    { value: 'BALANCED', label: 'Balanced', description: 'A helpful mix of context and brevity.' },
    { value: 'DETAILED', label: 'Detailed', description: 'More explanation and step-by-step guidance.' },
  ];

  readonly profileForm = new FormGroup({
    fullName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/.*\S.*/), Validators.maxLength(100)],
    }),
    phoneNumber: new FormControl('', {
      nonNullable: true,
      validators: [Validators.pattern(/^\+?[0-9]{7,15}$/)],
    }),
  });

  readonly preferencesForm = new FormGroup({
    taskRemindersEnabled: new FormControl(true, { nonNullable: true }),
    goalMilestoneUpdatesEnabled: new FormControl(true, { nonNullable: true }),
    habitRemindersEnabled: new FormControl(true, { nonNullable: true }),
    aiPlannerUpdatesEnabled: new FormControl(true, { nonNullable: true }),
    timeZoneId: new FormControl('DEVICE', { nonNullable: true }),
    dateFormat: new FormControl<DateFormatPreference>('LOCALE', { nonNullable: true }),
    timeFormat: new FormControl<TimeFormatPreference>('LOCALE', { nonNullable: true }),
    weekStart: new FormControl<WeekStartPreference>('MONDAY', { nonNullable: true }),
    aiResponseStyle: new FormControl<AiResponseStyle>('BALANCED', { nonNullable: true }),
    aiPersonalizationEnabled: new FormControl(true, { nonNullable: true }),
  });

  readonly passwordForm = new FormGroup({
    currentPassword: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    newPassword: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(8), Validators.maxLength(100)],
    }),
    confirmPassword: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
  });

  readonly appVersion = packageJson.version;

  private profileId: number | null = null;

  constructor() {
    effect(() => {
      const profile = this.auth.currentUserProfile();
      if (!profile || profile.id === this.profileId) return;
      this.profileId = profile.id;
      this.profileForm.reset({
        fullName: profile.fullName,
        phoneNumber: profile.phoneNumber ?? '',
      });
    });

    effect(() => {
      const profile = this.auth.currentUserProfile();
      const settings = this.userSettings.value();
      if (!profile || this.userSettings.loading()) return;
      this.preferencesForm.patchValue(settings, { emitEvent: false });
    });
  }

  openSection(section: SettingsSection): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { section },
      queryParamsHandling: 'merge',
    });
  }

  backToOverview(): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { section: null },
      queryParamsHandling: 'merge',
    });
  }

  sectionTitle(section: SettingsSection | null): string {
    return this.sectionCards.find((card) => card.id === section)?.title ?? 'Settings';
  }

  profileInitials(): string {
    return (this.profile()?.fullName ?? '')
      .trim()
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join('');
  }

  cancelProfileEdit(): void {
    const profile = this.profile();
    if (!profile) return;
    this.profileForm.reset({
      fullName: profile.fullName,
      phoneNumber: profile.phoneNumber ?? '',
    });
    this.profileError.set(null);
    this.profileSuccess.set(null);
  }

  saveProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }
    const { fullName, phoneNumber } = this.profileForm.getRawValue();
    this.profileSaving.set(true);
    this.profileError.set(null);
    this.profileSuccess.set(null);
    this.auth
      .updateMyProfile({
        fullName: fullName.trim(),
        phoneNumber: phoneNumber.trim() || null,
      })
      .pipe(finalize(() => this.profileSaving.set(false)))
      .subscribe({
        next: () => this.profileSuccess.set('Your profile has been updated.'),
        error: (error: unknown) =>
          this.profileError.set(readApiError(error, 'Could not save your profile.')),
      });
  }

  setTheme(mode: ThemeMode): void {
    this.theme.set(mode);
  }

  cancelPreferenceChanges(): void {
    this.preferencesForm.reset(this.userSettings.value());
    this.preferencesError.set(null);
    this.preferencesSuccess.set(null);
  }

  savePreferences(): void {
    if (this.preferencesForm.invalid) {
      this.preferencesForm.markAllAsTouched();
      return;
    }
    const settings: UserSettings = this.preferencesForm.getRawValue();
    this.preferencesSaving.set(true);
    this.preferencesError.set(null);
    this.preferencesSuccess.set(null);
    this.userSettings
      .save(settings)
      .pipe(finalize(() => this.preferencesSaving.set(false)))
      .subscribe({
        next: () => this.preferencesSuccess.set('Your preferences have been saved.'),
        error: (error: unknown) =>
          this.preferencesError.set(readApiError(error, 'Could not save your preferences.')),
      });
  }

  passwordConfirmationMismatch(): boolean {
    const { newPassword, confirmPassword } = this.passwordForm.getRawValue();
    return confirmPassword.length > 0 && newPassword !== confirmPassword;
  }

  changePassword(): void {
    if (this.passwordForm.invalid || this.passwordConfirmationMismatch()) {
      this.passwordForm.markAllAsTouched();
      return;
    }
    this.passwordSaving.set(true);
    this.passwordError.set(null);
    this.passwordSuccess.set(null);
    this.auth
      .changeMyPassword(this.passwordForm.getRawValue())
      .pipe(finalize(() => this.passwordSaving.set(false)))
      .subscribe({
        next: () => {
          this.passwordForm.reset();
          this.passwordSuccess.set('Your password has been changed.');
        },
        error: (error: unknown) =>
          this.passwordError.set(readApiError(error, 'Could not change your password.')),
      });
  }

  logout(): void {
    this.loggingOut.set(true);
    this.logoutError.set(null);
    this.auth
      .logout()
      .pipe(finalize(() => this.loggingOut.set(false)))
      .subscribe({
        error: (error: unknown) =>
          this.logoutError.set(readApiError(error, 'Could not sign out. Please try again.')),
      });
  }

  retryPreferenceLoad(): void {
    void this.userSettings.loadForCurrentUser();
  }
}

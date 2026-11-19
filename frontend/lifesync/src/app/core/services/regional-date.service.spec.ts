import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { UserSettingsStateService } from '../../features/settings/data/user-settings-state.service';
import { DEFAULT_USER_SETTINGS, UserSettings } from '../../features/settings/data/user-settings.models';
import { RegionalDateService } from './regional-date.service';

describe('RegionalDateService', () => {
  const settings = signal<UserSettings>({ ...DEFAULT_USER_SETTINGS });

  beforeEach(() => {
    settings.set({ ...DEFAULT_USER_SETTINGS });
    TestBed.configureTestingModule({
      providers: [
        { provide: UserSettingsStateService, useValue: { value: settings } },
      ],
    });
  });

  it('applies the selected date order and clock format', () => {
    const service = TestBed.inject(RegionalDateService);
    settings.set({ ...DEFAULT_USER_SETTINGS, dateFormat: 'MM_DD_YYYY', timeFormat: 'H24' });

    expect(service.formatDateOnly('2026-01-09', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    })).toBe('01/09/2026');
    expect(service.formatDateOnly('2026-01-09')).toBe('01/09/2026');
    expect(service.formatTime(new Date(2026, 0, 9, 13, 5))).toBe('13:05');
  });

  it('updates the calendar week start preference', () => {
    const service = TestBed.inject(RegionalDateService);

    expect(service.weekStartIndex()).toBe(1);
    settings.set({ ...DEFAULT_USER_SETTINGS, weekStart: 'SUNDAY' });
    expect(service.weekStartIndex()).toBe(0);
  });
});

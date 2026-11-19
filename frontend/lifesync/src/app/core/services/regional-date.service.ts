import { Injectable, inject } from '@angular/core';
import {
  DateFormatPreference,
  UserSettings,
} from '../../features/settings/data/user-settings.models';
import { UserSettingsStateService } from '../../features/settings/data/user-settings-state.service';

@Injectable({ providedIn: 'root' })
export class RegionalDateService {
  private readonly settings = inject(UserSettingsStateService);

  formatDateOnly(value: string | Date, options: Intl.DateTimeFormatOptions = { dateStyle: 'medium' }): string {
    const date = this.parseDateOnly(value);
    if (!date) return typeof value === 'string' ? value : '';
    return new Intl.DateTimeFormat(this.locale(), this.options(options, false, true)).format(date);
  }

  formatDateTime(
    value: string | Date,
    options: Intl.DateTimeFormatOptions = { dateStyle: 'medium', timeStyle: 'short' },
  ): string {
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) return typeof value === 'string' ? value : '';
    return new Intl.DateTimeFormat(this.locale(), this.options(options, true, true)).format(date);
  }

  formatTime(
    value: string | Date,
    options: Intl.DateTimeFormatOptions = { hour: 'numeric', minute: '2-digit' },
  ): string {
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) return typeof value === 'string' ? value : '';
    return new Intl.DateTimeFormat(this.locale(), this.options(options, true, false)).format(date);
  }

  weekStartIndex(): number {
    return this.settings.value().weekStart === 'SUNDAY' ? 0 : 1;
  }

  private parseDateOnly(value: string | Date): Date | null {
    if (value instanceof Date) {
      return new Date(value.getFullYear(), value.getMonth(), value.getDate());
    }
    const match = value.match(/^(\d{4})-(\d{2})-(\d{2})$/);
    if (match) {
      const date = new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]));
      return Number.isNaN(date.getTime()) ? null : date;
    }
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? null : date;
  }

  private locale(): string | undefined {
    return this.localeForDateFormat(this.settings.value().dateFormat);
  }

  private localeForDateFormat(format: DateFormatPreference): string | undefined {
    switch (format) {
      case 'MM_DD_YYYY':
        return 'en-US';
      case 'DD_MM_YYYY':
        return 'en-GB';
      case 'YYYY_MM_DD':
        return 'en-CA';
      default:
        return undefined;
    }
  }

  private options(
    options: Intl.DateTimeFormatOptions,
    includeTimeZone: boolean,
    includeDateFormat: boolean,
  ): Intl.DateTimeFormatOptions {
    const settings = this.settings.value();
    const result = { ...options };
    const hasFullDate =
      result.dateStyle !== undefined ||
      (result.year !== undefined && result.month !== undefined && result.day !== undefined);
    if (includeDateFormat && hasFullDate && settings.dateFormat !== 'LOCALE') {
      delete result.dateStyle;
      result.year = 'numeric';
      result.month = '2-digit';
      result.day = '2-digit';
    }
    if (includeTimeZone && settings.timeZoneId !== 'DEVICE') {
      result.timeZone = settings.timeZoneId;
    }
    if (this.includesTime(options) && settings.timeFormat !== 'LOCALE') {
      result.hour12 = this.uses12HourClock(settings);
    }
    return result;
  }

  private includesTime(options: Intl.DateTimeFormatOptions): boolean {
    return options.timeStyle !== undefined || options.hour !== undefined;
  }

  private uses12HourClock(settings: UserSettings): boolean {
    return settings.timeFormat === 'H12';
  }
}

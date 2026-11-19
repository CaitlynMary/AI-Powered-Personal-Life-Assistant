export type DateFormatPreference = 'LOCALE' | 'MM_DD_YYYY' | 'DD_MM_YYYY' | 'YYYY_MM_DD';
export type TimeFormatPreference = 'LOCALE' | 'H12' | 'H24';
export type WeekStartPreference = 'SUNDAY' | 'MONDAY';
export type AiResponseStyle = 'CONCISE' | 'BALANCED' | 'DETAILED';

export interface UserSettings {
  taskRemindersEnabled: boolean;
  goalMilestoneUpdatesEnabled: boolean;
  habitRemindersEnabled: boolean;
  aiPlannerUpdatesEnabled: boolean;
  timeZoneId: string;
  dateFormat: DateFormatPreference;
  timeFormat: TimeFormatPreference;
  weekStart: WeekStartPreference;
  aiResponseStyle: AiResponseStyle;
  aiPersonalizationEnabled: boolean;
}

export const DEFAULT_USER_SETTINGS: UserSettings = {
  taskRemindersEnabled: true,
  goalMilestoneUpdatesEnabled: true,
  habitRemindersEnabled: true,
  aiPlannerUpdatesEnabled: true,
  timeZoneId: 'DEVICE',
  dateFormat: 'LOCALE',
  timeFormat: 'LOCALE',
  weekStart: 'MONDAY',
  aiResponseStyle: 'BALANCED',
  aiPersonalizationEnabled: true,
};

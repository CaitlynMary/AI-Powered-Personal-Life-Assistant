import { CommonModule } from '@angular/common';
import { Component, inject, output, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { finalize } from 'rxjs';
import { readApiError } from '../../../core/auth/auth-error';
import { RegionalDateService } from '../../../core/services/regional-date.service';
import { AiAssistantApiService } from '../../ai-assistant/data/ai-assistant-api.service';
import {
  DailyPlanRequest,
  DailyPlanResponse,
  isDailyPlanResponse,
} from '../../ai-assistant/data/ai-assistant.models';
import { ScheduleApiService } from '../../calendar/data/schedule-api.service';

function localDate(date = new Date()): string {
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function isValidPlanWindow(control: AbstractControl): ValidationErrors | null {
  const date = control.get('date')?.value as string | undefined;
  const start = control.get('dayStartTime')?.value as string | undefined;
  const end = control.get('dayEndTime')?.value as string | undefined;
  if (!date || !start || !end) return null;
  if (date < localDate()) return { pastDate: true };
  if (start >= end) return { invalidTimeWindow: true };
  if (date === localDate() && start < localDateTimeNow()) return { startTimeInPast: true };
  return null;
}

function localDateTimeNow(): string {
  const now = new Date();
  return `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
}

@Component({
  selector: 'app-daily-plan-panel',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './daily-plan-panel.html',
})
export class DailyPlanPanel {
  private readonly formBuilder = inject(FormBuilder);
  private readonly api = inject(AiAssistantApiService);
  private readonly scheduleApi = inject(ScheduleApiService);
  private readonly regionalDate = inject(RegionalDateService);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly plan = signal<DailyPlanResponse | null>(null);
  readonly approving = signal(false);
  readonly approvalMessage = signal<string | null>(null);
  readonly approved = output<void>();
  readonly form = this.formBuilder.nonNullable.group(
    {
      date: [localDate(), Validators.required],
      dayStartTime: ['', Validators.required],
      dayEndTime: ['', Validators.required],
    },
    { validators: isValidPlanWindow },
  );

  get minimumDate(): string {
    return localDate();
  }

  generatePlan(): void {
    if (this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    const request: DailyPlanRequest = this.form.getRawValue();
    this.loading.set(true);
    this.error.set(null);
    this.approvalMessage.set(null);
    this.plan.set(null);
    this.api
      .createDailyPlan(request)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (response) => {
          if (!isDailyPlanResponse(response, request.date)) {
            this.error.set(
              'The AI returned an incomplete or invalid plan. Your records were not changed; please regenerate the draft.',
            );
            return;
          }
          this.plan.set(response);
        },
        error: (error: unknown) => {
          this.error.set(readApiError(error, 'Could not generate a daily plan. Please try again.'));
        },
      });
  }

  discardPlan(): void {
    this.plan.set(null);
    this.error.set(null);
    this.approvalMessage.set(null);
  }

  updateSuggestion(
    taskId: number,
    field: 'startTime' | 'endTime',
    value: string,
  ): void {
    const plan = this.plan();
    if (!plan) return;
    this.plan.set({
      ...plan,
      suggestions: plan.suggestions.map((suggestion) =>
        suggestion.taskId === taskId ? { ...suggestion, [field]: value } : suggestion,
      ),
    });
    this.approvalMessage.set(null);
  }

  onTimeChange(taskId: number, field: 'startTime' | 'endTime', event: Event): void {
    const input = event.target;
    if (input instanceof HTMLInputElement) {
      this.updateSuggestion(taskId, field, input.value);
    }
  }

  removeSuggestion(taskId: number): void {
    const plan = this.plan();
    if (!plan) return;
    this.plan.set({
      ...plan,
      suggestions: plan.suggestions.filter((suggestion) => suggestion.taskId !== taskId),
    });
    this.approvalMessage.set(null);
  }

  approvePlan(): void {
    const plan = this.plan();
    if (!plan || !plan.suggestions.length || this.approving()) return;
    this.approving.set(true);
    this.error.set(null);
    this.approvalMessage.set(null);
    this.scheduleApi
      .approveDailyPlan({
        date: plan.date,
        items: plan.suggestions.map((suggestion) => ({
          taskId: suggestion.taskId,
          startTime: suggestion.startTime,
          endTime: suggestion.endTime,
        })),
      })
      .pipe(finalize(() => this.approving.set(false)))
      .subscribe({
        next: (approved) => {
          this.approvalMessage.set(
            `${approved.length} task${approved.length === 1 ? '' : 's'} added to your schedule.`,
          );
          this.plan.set(null);
          this.approved.emit();
        },
        error: (error: unknown) => {
          this.error.set(readApiError(error, 'Could not approve this plan. No changes were saved.'));
        },
      });
  }

  formatTime(value: string): string {
    const [hours, minutes] = value.split(':').map(Number);
    if (!Number.isFinite(hours) || !Number.isFinite(minutes)) return value;
    const date = new Date();
    date.setHours(hours, minutes, 0, 0);
    return this.regionalDate.formatTime(date, { hour: 'numeric', minute: '2-digit' });
  }

  priorityLabel(value: string): string {
    return value.charAt(0) + value.slice(1).toLowerCase();
  }
}

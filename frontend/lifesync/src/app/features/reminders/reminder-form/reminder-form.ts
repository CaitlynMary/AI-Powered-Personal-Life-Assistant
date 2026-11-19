import { Component, effect, inject, input, output, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { TaskApiService } from '../../tasks/data/task-api.service';
import { TaskResponse } from '../../tasks/data/task.models';
import { readApiError } from '../../../core/auth/auth-error';
import { finalize } from 'rxjs';
import { ReminderRequest, ReminderResponse, ReminderType } from '../data/reminder.models';

function nonBlank(control: AbstractControl): ValidationErrors | null {
  return typeof control.value === 'string' && !control.value.trim() ? { required: true } : null;
}

function validDateTime(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  return value && Number.isNaN(new Date(value).getTime()) ? { invalidDateTime: true } : null;
}

const REMINDER_OFFSETS = [
  { value: '1_DAY', label: '1 day before', minutes: 1440 },
  { value: '3_HOURS', label: '3 hours before', minutes: 180 },
  { value: '1_HOUR', label: '1 hour before', minutes: 60 },
  { value: '15_MINUTES', label: '15 minutes before', minutes: 15 },
  { value: 'AT_DEADLINE', label: 'At the deadline', minutes: 0 },
  { value: 'CUSTOM', label: 'Custom date and time', minutes: null },
] as const;

@Component({
  selector: 'app-reminder-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './reminder-form.html',
})
export class ReminderForm {
  private readonly formBuilder = inject(FormBuilder);
  private readonly tasksApi = inject(TaskApiService);

  readonly reminder = input<ReminderResponse | null>(null);
  readonly saving = input(false);
  readonly requestError = input<string | null>(null);
  readonly save = output<{ id: number | null; request: ReminderRequest }>();
  readonly cancel = output<void>();
  readonly tasks = signal<TaskResponse[]>([]);
  readonly taskLoading = signal(true);
  readonly taskLoadError = signal<string | null>(null);
  readonly validationError = signal<string | null>(null);
  readonly offsets = REMINDER_OFFSETS;

  readonly form = this.formBuilder.nonNullable.group({
    reminderKind: ['STANDALONE' as 'STANDALONE' | 'TASK'],
    taskId: [0],
    reminderOffset: ['1_HOUR' as (typeof REMINDER_OFFSETS)[number]['value']],
    title: ['', [Validators.required, nonBlank, Validators.maxLength(200)]],
    message: ['', [Validators.required, nonBlank, Validators.maxLength(2000)]],
    reminderDateTime: ['', [Validators.required, validDateTime]],
    reminderType: ['GENERAL' as ReminderType, Validators.required],
  });

  constructor() {
    this.loadTasks();
    effect(() => {
      const reminder = this.reminder();
      const savedOffset = this.offsets.find(
        (offset) => offset.minutes === reminder?.deadlineOffsetMinutes,
      );
      this.form.reset({
        reminderKind: reminder?.taskId ? 'TASK' : 'STANDALONE',
        taskId: reminder?.taskId ?? 0,
        reminderOffset: savedOffset?.value ?? 'CUSTOM',
        title: reminder?.title ?? '',
        message: reminder?.message ?? '',
        reminderDateTime: reminder?.reminderDateTime.slice(0, 16) ?? '',
        reminderType: reminder?.reminderType ?? 'GENERAL',
      });
    });
  }

  loadTasks(): void {
    this.taskLoading.set(true);
    this.taskLoadError.set(null);
    this.tasksApi
      .listAll()
      .pipe(finalize(() => this.taskLoading.set(false)))
      .subscribe({
        next: (tasks) => this.tasks.set(tasks),
        error: (error: unknown) =>
          this.taskLoadError.set(readApiError(error, 'Could not load tasks for reminders.')),
      });
  }

  availableTasks(): TaskResponse[] {
    return this.tasks().filter(
      (task) =>
        task.dueDate !== null &&
        (task.status === 'TODO' || task.status === 'IN_PROGRESS' || task.status === 'OVERDUE'),
    );
  }

  selectedTask(): TaskResponse | null {
    return this.availableTasks().find((task) => task.id === this.form.controls.taskId.value) ?? null;
  }

  onReminderKindChange(): void {
    this.validationError.set(null);
    if (this.form.controls.reminderKind.value === 'TASK') {
      this.form.controls.reminderType.setValue('TASK');
      this.onTaskChange();
    } else {
      this.form.controls.taskId.setValue(0);
      this.form.controls.reminderType.setValue('GENERAL');
    }
  }

  onTaskChange(): void {
    const task = this.selectedTask();
    if (!task?.dueDate) return;
    this.form.controls.title.setValue(task.title);
    this.form.controls.message.setValue(`Reminder for task: ${task.title}`);
    this.form.controls.reminderType.setValue('TASK');
    if (this.form.controls.reminderOffset.value !== 'CUSTOM') this.applyOffset();
  }

  onOffsetChange(): void {
    this.validationError.set(null);
    if (this.form.controls.reminderOffset.value !== 'CUSTOM') this.applyOffset();
  }

  private applyOffset(): void {
    const task = this.selectedTask();
    if (!task?.dueDate) return;
    const offset = this.offsets.find((item) => item.value === this.form.controls.reminderOffset.value);
    if (!offset || offset.minutes === null) return;
    const date = new Date(task.dueDate);
    date.setMinutes(date.getMinutes() - offset.minutes);
    this.form.controls.reminderDateTime.setValue(this.toLocalDateTime(date));
  }

  isDateInPast(): boolean {
    const value = this.form.controls.reminderDateTime.value;
    return Boolean(
      value && !Number.isNaN(new Date(value).getTime()) && new Date(value).getTime() < Date.now(),
    );
  }

  submit(): void {
    this.validationError.set(null);
    if (this.form.controls.reminderKind.value === 'TASK' && !this.selectedTask()) {
      this.validationError.set('Select an active task with a deadline.');
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.isDateInPast()) {
      this.validationError.set('Choose a reminder time that is in the future.');
      return;
    }
    const value = this.form.getRawValue();
    const task = value.reminderKind === 'TASK' ? this.selectedTask() : null;
    const offset = this.offsets.find((item) => item.value === value.reminderOffset);
    this.save.emit({
      id: this.reminder()?.id ?? null,
      request: {
        title: task?.title ?? value.title.trim(),
        message: task ? `Reminder for task: ${task.title}` : value.message.trim(),
        reminderDateTime: `${value.reminderDateTime}:00`,
        reminderType: task ? 'TASK' : value.reminderType,
        taskId: task?.id ?? null,
        ...(task ? { deadlineOffsetMinutes: offset?.minutes ?? null } : {}),
      },
    });
  }

  private toLocalDateTime(date: Date): string {
    const pad = (value: number) => String(value).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }
}

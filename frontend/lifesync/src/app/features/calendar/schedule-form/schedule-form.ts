import { Component, effect, inject, input, output } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import {
  CalendarTask,
  ScheduleConflictItem,
  ScheduleSlotRequest,
  ScheduleSlotResponse,
} from '../data/calendar.models';

function nonBlank(control: AbstractControl): ValidationErrors | null {
  return typeof control.value === 'string' && !control.value.trim() ? { required: true } : null;
}

function intervalValidator(control: AbstractControl): ValidationErrors | null {
  const start = control.get('startDateTime')?.value as string;
  const end = control.get('endDateTime')?.value as string;
  return start && end && end <= start ? { invalidInterval: true } : null;
}

@Component({
  selector: 'app-schedule-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './schedule-form.html',
})
export class ScheduleForm {
  private readonly formBuilder = inject(FormBuilder);

  readonly slot = input<ScheduleSlotResponse | null>(null);
  readonly initialTaskId = input<number | null>(null);
  readonly tasks = input<CalendarTask[]>([]);
  readonly conflicts = input<ScheduleConflictItem[]>([]);
  readonly saving = input(false);
  readonly requestError = input<string | null>(null);
  readonly save = output<{ id: number | null; request: ScheduleSlotRequest }>();
  readonly cancel = output<void>();

  readonly form = this.formBuilder.nonNullable.group(
    {
      title: ['', [Validators.required, nonBlank, Validators.maxLength(150)]],
      description: ['', [Validators.maxLength(2000)]],
      startDateTime: ['', Validators.required],
      endDateTime: ['', Validators.required],
      taskId: [''],
    },
    { validators: intervalValidator },
  );

  constructor() {
    effect(() => {
      const slot = this.slot();
      const taskId = slot?.taskId ?? this.initialTaskId();
      this.form.reset({
        title: slot?.title ?? '',
        description: slot?.description ?? '',
        startDateTime: this.toInput(slot?.startDateTime),
        endDateTime: this.toInput(slot?.endDateTime),
        taskId: taskId == null ? '' : String(taskId),
      });
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.save.emit({
      id: this.slot()?.id ?? null,
      request: {
        title: value.title.trim(),
        description: value.description.trim() || null,
        startDateTime: this.toBackend(value.startDateTime),
        endDateTime: this.toBackend(value.endDateTime),
        taskId: value.taskId ? Number(value.taskId) : null,
      },
    });
  }

  conflictTypeLabel(type: string): string {
    return (
      {
        SCHEDULE_SLOT: 'Schedule entry',
        CALENDAR_EVENT: 'Calendar event',
        TASK: 'Task',
      }[type] ?? type
    );
  }

  private toInput(value: string | null | undefined): string {
    return value ? value.slice(0, 16) : '';
  }

  private toBackend(value: string): string {
    return `${value}:00`;
  }
}

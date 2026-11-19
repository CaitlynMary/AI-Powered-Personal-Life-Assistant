import { Component, effect, inject, input, output } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import {
  CalendarEventRequest,
  CalendarEventResponse,
  CalendarEventType,
  CalendarRecurrenceType,
  CalendarTask,
} from '../data/calendar.models';

function nonBlank(control: AbstractControl): ValidationErrors | null {
  return typeof control.value === 'string' && !control.value.trim() ? { required: true } : null;
}

function eventDateValidator(control: AbstractControl): ValidationErrors | null {
  const start = control.get('startDateTime')?.value as string;
  const end = control.get('endDateTime')?.value as string;
  return start && end && end <= start ? { invalidInterval: true } : null;
}

@Component({
  selector: 'app-calendar-event-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './event-form.html',
})
export class CalendarEventForm {
  private readonly formBuilder = inject(FormBuilder);

  readonly event = input<CalendarEventResponse | null>(null);
  readonly tasks = input<CalendarTask[]>([]);
  readonly saving = input(false);
  readonly requestError = input<string | null>(null);
  readonly save = output<{ id: number | null; request: CalendarEventRequest }>();
  readonly cancel = output<void>();

  readonly form = this.formBuilder.nonNullable.group(
    {
      title: ['', [Validators.required, nonBlank, Validators.maxLength(150)]],
      description: ['', [Validators.maxLength(2000)]],
      eventType: ['PERSONAL' as CalendarEventType, Validators.required],
      allDay: [false],
      startDateTime: ['', Validators.required],
      endDateTime: ['', Validators.required],
      recurrenceType: ['NONE' as CalendarRecurrenceType, Validators.required],
      taskId: [''],
    },
    { validators: eventDateValidator },
  );

  constructor() {
    effect(() => {
      const event = this.event();
      this.form.reset({
        title: event?.title ?? '',
        description: event?.description ?? '',
        eventType: event?.eventType ?? 'PERSONAL',
        allDay: event?.allDay ?? false,
        startDateTime: this.toInput(event?.startDateTime),
        endDateTime: this.toInput(event?.endDateTime),
        recurrenceType: event?.recurrenceType ?? 'NONE',
        taskId: event?.taskId == null ? '' : String(event.taskId),
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
      id: this.event()?.id ?? null,
      request: {
        title: value.title.trim(),
        description: value.description.trim() || null,
        eventType: value.eventType,
        allDay: value.allDay,
        startDateTime: this.toBackend(value.startDateTime),
        endDateTime: this.toBackend(value.endDateTime),
        recurrenceType: value.recurrenceType,
        taskId: value.taskId ? Number(value.taskId) : null,
      },
    });
  }

  private toInput(value: string | null | undefined): string {
    return value ? value.slice(0, 16) : '';
  }

  private toBackend(value: string): string {
    return `${value}:00`;
  }
}

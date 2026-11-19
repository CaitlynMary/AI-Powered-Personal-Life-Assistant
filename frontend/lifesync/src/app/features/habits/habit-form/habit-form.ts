import { Component, effect, inject, input, output } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { HabitFrequency, HabitRequest, HabitResponse } from '../data/habit.models';

function nonBlank(control: AbstractControl): ValidationErrors | null {
  return typeof control.value === 'string' && !control.value.trim() ? { required: true } : null;
}

function habitDateValidator(control: AbstractControl): ValidationErrors | null {
  const startDate = control.get('startDate')?.value as string;
  const endDate = control.get('endDate')?.value as string;
  return startDate && endDate && endDate < startDate ? { endBeforeStart: true } : null;
}

@Component({
  selector: 'app-habit-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './habit-form.html',
})
export class HabitForm {
  private readonly formBuilder = inject(FormBuilder);

  readonly habit = input<HabitResponse | null>(null);
  readonly saving = input(false);
  readonly requestError = input<string | null>(null);
  readonly save = output<{ id: number | null; request: HabitRequest }>();
  readonly cancel = output<void>();

  readonly form = this.formBuilder.nonNullable.group(
    {
      name: ['', [Validators.required, nonBlank, Validators.maxLength(150)]],
      description: ['', [Validators.maxLength(2000)]],
      frequency: ['DAILY' as HabitFrequency, Validators.required],
      targetDaysPerWeek: [7, [Validators.required, Validators.min(1), Validators.max(7)]],
      startDate: ['', Validators.required],
      endDate: [''],
    },
    { validators: habitDateValidator },
  );

  constructor() {
    effect(() => {
      const habit = this.habit();
      this.form.reset({
        name: habit?.name ?? '',
        description: habit?.description ?? '',
        frequency: habit?.frequency ?? 'DAILY',
        targetDaysPerWeek: habit?.targetDaysPerWeek ?? 7,
        startDate: habit?.startDate ?? '',
        endDate: habit?.endDate ?? '',
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
      id: this.habit()?.id ?? null,
      request: {
        name: value.name.trim(),
        description: value.description.trim() || null,
        frequency: value.frequency,
        targetDaysPerWeek: value.targetDaysPerWeek,
        startDate: value.startDate,
        endDate: value.endDate || null,
      },
    });
  }
}

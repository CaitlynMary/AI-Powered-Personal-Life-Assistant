import { Component, effect, inject, input, output } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { GoalRequest, GoalResponse, GoalType } from '../data/goal.models';

function nonBlank(control: AbstractControl): ValidationErrors | null {
  return typeof control.value === 'string' && !control.value.trim() ? { required: true } : null;
}

function goalDatesValidator(control: AbstractControl): ValidationErrors | null {
  const startDate = control.get('startDate')?.value as string;
  const targetDate = control.get('targetDate')?.value as string;
  return startDate && targetDate && targetDate < startDate ? { targetBeforeStart: true } : null;
}

@Component({
  selector: 'app-goal-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './goal-form.html',
})
export class GoalForm {
  private readonly formBuilder = inject(FormBuilder);

  readonly goal = input<GoalResponse | null>(null);
  readonly saving = input(false);
  readonly requestError = input<string | null>(null);
  readonly save = output<{ id: number | null; request: GoalRequest }>();
  readonly cancel = output<void>();

  readonly form = this.formBuilder.nonNullable.group(
    {
      title: ['', [Validators.required, nonBlank, Validators.maxLength(150)]],
      description: ['', [Validators.maxLength(2000)]],
      goalType: ['MONTHLY' as GoalType, Validators.required],
      startDate: ['', Validators.required],
      targetDate: ['', Validators.required],
    },
    { validators: goalDatesValidator },
  );

  constructor() {
    effect(() => {
      const goal = this.goal();
      this.form.reset({
        title: goal?.title ?? '',
        description: goal?.description ?? '',
        goalType: goal?.goalType ?? 'MONTHLY',
        startDate: goal?.startDate ?? '',
        targetDate: goal?.targetDate ?? '',
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
      id: this.goal()?.id ?? null,
      request: {
        title: value.title.trim(),
        description: value.description.trim() || null,
        goalType: value.goalType,
        startDate: value.startDate,
        targetDate: value.targetDate,
      },
    });
  }
}

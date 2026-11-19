import { Component, effect, inject, input, output } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MilestoneRequest, MilestoneResponse } from '../data/goal.models';

function nonBlank(control: AbstractControl): ValidationErrors | null {
  return typeof control.value === 'string' && !control.value.trim() ? { required: true } : null;
}

function dateRangeValidator(startDate: string, targetDate: string) {
  return (control: AbstractControl): ValidationErrors | null => {
    const date = control.value as string;
    return date && (date < startDate || date > targetDate) ? { outsideGoalRange: true } : null;
  };
}

@Component({
  selector: 'app-milestone-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './milestone-form.html',
})
export class MilestoneForm {
  private readonly formBuilder = inject(FormBuilder);

  readonly milestone = input<MilestoneResponse | null>(null);
  readonly goalStartDate = input.required<string>();
  readonly goalTargetDate = input.required<string>();
  readonly saving = input(false);
  readonly requestError = input<string | null>(null);
  readonly save = output<{ id: number | null; request: MilestoneRequest }>();
  readonly cancel = output<void>();

  readonly form = this.formBuilder.nonNullable.group({
    title: ['', [Validators.required, nonBlank, Validators.maxLength(150)]],
    description: ['', [Validators.maxLength(2000)]],
    targetDate: ['', Validators.required],
  });

  constructor() {
    effect(() => {
      const milestone = this.milestone();
      this.form.controls.targetDate.setValidators([
        Validators.required,
        dateRangeValidator(this.goalStartDate(), this.goalTargetDate()),
      ]);
      this.form.reset({
        title: milestone?.title ?? '',
        description: milestone?.description ?? '',
        targetDate: milestone?.targetDate ?? '',
      });
      this.form.controls.targetDate.updateValueAndValidity({ emitEvent: false });
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.save.emit({
      id: this.milestone()?.id ?? null,
      request: {
        title: value.title.trim(),
        description: value.description.trim() || null,
        targetDate: value.targetDate,
      },
    });
  }
}

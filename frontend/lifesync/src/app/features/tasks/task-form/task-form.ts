import { Component, effect, input, output, inject } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { CategoryResponse, RecurrenceType, TaskRequest, TaskResponse } from '../data/task.models';

function nonBlank(control: AbstractControl): ValidationErrors | null {
  return typeof control.value === 'string' && !control.value.trim()
    ? { required: true }
    : null;
}

function taskFieldsValidator(control: AbstractControl): ValidationErrors | null {
  const startDate = control.get('startDate')?.value as string;
  const dueDate = control.get('dueDate')?.value as string;
  const recurring = control.get('recurring')?.value as boolean;
  const recurrenceType = control.get('recurrenceType')?.value as RecurrenceType;
  const errors: ValidationErrors = {};

  if (startDate && dueDate && startDate > dueDate) {
    errors['startAfterDue'] = true;
  }
  if (recurring && recurrenceType === 'NONE') {
    errors['recurrenceRequired'] = true;
  }

  return Object.keys(errors).length ? errors : null;
}

@Component({
  selector: 'app-task-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './task-form.html',
})
export class TaskForm {
  private readonly formBuilder = inject(FormBuilder);

  readonly task = input<TaskResponse | null>(null);
  readonly categories = input<CategoryResponse[]>([]);
  readonly saving = input(false);
  readonly requestError = input<string | null>(null);
  readonly save = output<{ id: number | null; request: TaskRequest }>();
  readonly cancel = output<void>();

  readonly form = this.formBuilder.nonNullable.group(
    {
      title: ['', [Validators.required, nonBlank, Validators.maxLength(150)]],
      description: ['', [Validators.maxLength(2000)]],
      categoryId: [''],
      priority: ['MEDIUM' as 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT', Validators.required],
      dueDate: [''],
      startDate: [''],
      estimatedDuration: ['', [Validators.pattern(/^\d+$/), Validators.min(1)]],
      recurring: [false],
      recurrenceType: ['NONE' as RecurrenceType],
    },
    { validators: taskFieldsValidator },
  );

  constructor() {
    effect(() => {
      const task = this.task();
      this.form.reset({
        title: task?.title ?? '',
        description: task?.description ?? '',
        categoryId: task?.categoryId ? String(task.categoryId) : '',
        priority: task?.priority ?? 'MEDIUM',
        dueDate: this.toDateTimeInput(task?.dueDate),
        startDate: this.toDateTimeInput(task?.startDate),
        estimatedDuration:
          task?.estimatedDuration == null ? '' : String(task.estimatedDuration),
        recurring: task?.recurring ?? false,
        recurrenceType: task?.recurrenceType ?? 'NONE',
      });
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const currentTask = this.task();
    this.save.emit({
      id: currentTask?.id ?? null,
      request: {
        title: value.title.trim(),
        description: value.description.trim() || null,
        categoryId: value.categoryId ? Number(value.categoryId) : null,
        goalId: currentTask?.goalId ?? null,
        milestoneId: currentTask?.milestoneId ?? null,
        priority: value.priority,
        dueDate: this.toBackendDateTime(value.dueDate),
        startDate: this.toBackendDateTime(value.startDate),
        estimatedDuration: value.estimatedDuration ? Number(value.estimatedDuration) : null,
        recurring: value.recurring,
        recurrenceType: value.recurring ? value.recurrenceType : 'NONE',
      },
    });
  }

  private toDateTimeInput(value: string | null | undefined): string {
    return value ? value.slice(0, 16) : '';
  }

  private toBackendDateTime(value: string): string | null {
    return value ? `${value}:00` : null;
  }
}

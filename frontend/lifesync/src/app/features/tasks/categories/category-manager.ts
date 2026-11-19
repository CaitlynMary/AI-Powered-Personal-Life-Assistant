import { Component, effect, inject, input, output, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { finalize } from 'rxjs';
import { readApiError } from '../../../core/auth/auth-error';
import { ConfirmationDialogService } from '../../../shared/confirmation-dialog/confirmation-dialog.service';
import { CategoryApiService } from '../data/category-api.service';
import { CategoryRequest, CategoryResponse } from '../data/task.models';

function nonBlank(control: AbstractControl): ValidationErrors | null {
  return typeof control.value === 'string' && !control.value.trim()
    ? { required: true }
    : null;
}

@Component({
  selector: 'app-category-manager',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <section class="ls-card bg-ls-surface p-5 sm:p-6">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p class="text-xs font-extrabold uppercase tracking-[0.14em] text-purple">Organise</p>
          <h2 class="mt-1 text-xl font-extrabold text-plum dark:text-cream">Categories</h2>
        </div>
        <button
          type="button"
          class="rounded-xl bg-lime px-4 py-2.5 text-sm font-bold text-plum hover:bg-mint"
          (click)="beginCreate()"
        >
          Add category
        </button>
      </div>

      @if (error()) {
        <p class="mt-4 rounded-xl bg-ls-surface-2 px-3 py-2 text-sm font-semibold text-ls-text" role="alert">
          {{ error() }}
        </p>
      }

      @if (editing()) {
        <form
          class="mt-4 grid gap-3 rounded-2xl bg-ls-bg p-4 sm:grid-cols-2"
          [formGroup]="form"
          (ngSubmit)="save()"
        >
          <label class="block">
            <span class="mb-1 block text-xs font-bold text-ls-text">Name *</span>
            <input
              formControlName="name"
              maxlength="100"
              class="h-10 w-full rounded-lg border border-ls-border bg-ls-surface px-3 text-sm text-ls-text"
            />
            @if (form.controls.name.touched && form.controls.name.invalid) {
              <span class="mt-1 block text-xs font-semibold text-burgundy">Name is required (100 characters max).</span>
            }
          </label>
          <label class="block">
            <span class="mb-1 block text-xs font-bold text-ls-text">Color</span>
            <input
              type="color"
              formControlName="color"
              class="h-10 w-full rounded-lg border border-ls-border bg-ls-surface px-2"
            />
          </label>
          <label class="block sm:col-span-2">
            <span class="mb-1 block text-xs font-bold text-ls-text">Description</span>
            <textarea
              formControlName="description"
              maxlength="500"
              rows="2"
              class="w-full rounded-lg border border-ls-border bg-ls-surface px-3 py-2 text-sm text-ls-text"
            ></textarea>
            @if (form.controls.description.touched && form.controls.description.hasError('maxlength')) {
              <span class="mt-1 block text-xs font-semibold text-burgundy">Description cannot exceed 500 characters.</span>
            }
          </label>
          <div class="flex justify-end gap-2 sm:col-span-2">
            <button type="button" class="rounded-lg px-4 py-2 text-sm font-bold text-ls-text-muted" (click)="cancelEdit()">
              Cancel
            </button>
            <button
              type="submit"
              class="rounded-lg bg-lime px-4 py-2 text-sm font-extrabold text-plum transition hover:bg-mint disabled:opacity-60"
              [disabled]="saving()"
            >
              {{ saving() ? 'Saving…' : editing() && editing() !== 'new' ? 'Save category' : 'Create category' }}
            </button>
          </div>
        </form>
      }

      @if (loading()) {
        <p class="py-5 text-sm font-medium text-ls-text-muted" role="status">Loading categories…</p>
      } @else if (categories().length === 0) {
        <p class="py-5 text-sm font-medium text-ls-text-muted">No categories yet. Add one to organise your tasks.</p>
      } @else {
        <ul class="mt-4 grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
          @for (category of categories(); track category.id) {
            <li class="flex items-center gap-3 rounded-xl border border-ls-border bg-ls-bg px-3 py-3">
              <span
                class="h-3 w-3 shrink-0 rounded-full"
                [style.backgroundColor]="category.color || '#8E1EA2'"
              ></span>
              <span class="min-w-0 flex-1">
                <span class="block truncate text-sm font-bold text-ls-text">{{ category.name }}</span>
                @if (category.description) {
                  <span class="block truncate text-xs text-ls-text-muted">{{ category.description }}</span>
                }
              </span>
              <button
                type="button"
                class="rounded-lg px-2 py-1 text-xs font-bold text-purple hover:bg-purple/10"
                (click)="beginEdit(category)"
              >
                Edit
              </button>
              <button
                type="button"
                class="rounded-lg px-2 py-1 text-xs font-bold text-burgundy hover:bg-ls-surface-2/50 disabled:opacity-50"
                [disabled]="deletingId() === category.id"
                (click)="remove(category)"
              >
                {{ deletingId() === category.id ? 'Deleting…' : 'Delete' }}
              </button>
            </li>
          }
        </ul>
      }
    </section>
  `,
})
export class CategoryManager {
  private readonly confirmation = inject(ConfirmationDialogService);
  private readonly api = inject(CategoryApiService);
  private readonly formBuilder = inject(FormBuilder);

  readonly categories = input<CategoryResponse[]>([]);
  readonly loading = input(false);
  readonly categoriesChanged = output<void>();
  readonly editing = signal<CategoryResponse | 'new' | null>(null);
  readonly saving = signal(false);
  readonly deletingId = signal<number | null>(null);
  readonly error = signal<string | null>(null);

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, nonBlank, Validators.maxLength(100)]],
    description: ['', [Validators.maxLength(500)]],
    color: ['#8E1EA2', [Validators.pattern(/^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6})$/)]],
  });

  constructor() {
    effect(() => {
      const current = this.editing();
      this.form.reset({
        name: current && current !== 'new' ? current.name : '',
        description: current && current !== 'new' ? current.description ?? '' : '',
        color: current && current !== 'new' ? current.color ?? '#8E1EA2' : '#8E1EA2',
      });
    });
  }

  beginCreate(): void {
    this.error.set(null);
    this.editing.set('new');
  }

  beginEdit(category: CategoryResponse): void {
    this.error.set(null);
    this.editing.set(category);
  }

  cancelEdit(): void {
    this.editing.set(null);
    this.error.set(null);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const request: CategoryRequest = {
      name: this.form.controls.name.value.trim(),
      description: this.form.controls.description.value.trim() || null,
      color: this.form.controls.color.value || null,
    };
    const current = this.editing();
    this.saving.set(true);
    this.error.set(null);

    const operation =
      current && current !== 'new'
        ? this.api.update(current.id, request)
        : this.api.create(request);
    operation.subscribe({
      next: () => {
        this.saving.set(false);
        this.editing.set(null);
        this.categoriesChanged.emit();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.error.set(readApiError(error, 'Could not save this category. Please try again.'));
      },
    });
  }

  async remove(category: CategoryResponse): Promise<void> {
    if (!(await this.confirmation.confirm({
      title: 'Delete this category?',
      message: `Delete category "${category.name}"? Tasks in this category will become uncategorised.`,
      confirmLabel: 'Yes, Delete',
    }))) return;
    this.deletingId.set(category.id);
    this.error.set(null);
    this.api
      .delete(category.id)
      .pipe(finalize(() => this.deletingId.set(null)))
      .subscribe({
        next: () => this.categoriesChanged.emit(),
        error: (error: unknown) => {
          this.error.set(readApiError(error, 'Could not delete this category. Please try again.'));
        },
      });
  }
}

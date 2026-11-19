import { Component, effect, inject, input, output } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { NoteRequest, NoteResponse } from '../data/note.models';

export type NoteEditorMode = 'CREATE' | 'EDIT' | 'VIEW';

function nonBlank(control: AbstractControl): ValidationErrors | null {
  return typeof control.value === 'string' && !control.value.trim() ? { required: true } : null;
}

@Component({
  selector: 'app-note-editor',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './note-editor.html',
})
export class NoteEditor {
  private readonly formBuilder = inject(FormBuilder);

  readonly note = input<NoteResponse | null>(null);
  readonly mode = input<NoteEditorMode>('CREATE');
  readonly saving = input(false);
  readonly requestError = input<string | null>(null);
  readonly save = output<{ id: number | null; request: NoteRequest }>();
  readonly close = output<void>();
  readonly editRequested = output<void>();

  readonly form = this.formBuilder.nonNullable.group({
    title: ['', [Validators.required, nonBlank, Validators.maxLength(200)]],
    content: ['', [Validators.required, nonBlank, Validators.maxLength(50000)]],
  });

  constructor() {
    effect(() => {
      const note = this.note();
      this.form.reset({
        title: note?.title ?? '',
        content: note?.content ?? '',
      });
    });
  }

  submit(): void {
    if (this.mode() === 'VIEW') return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.save.emit({
      id: this.note()?.id ?? null,
      request: {
        title: value.title.trim(),
        content: value.content.trim(),
        noteType: this.note()?.noteType ?? 'GENERAL',
        journalDate: this.note()?.journalDate ?? null,
      },
    });
  }
}

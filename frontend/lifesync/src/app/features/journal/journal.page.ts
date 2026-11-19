import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormsModule,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { finalize } from 'rxjs';
import { readApiError } from '../../core/auth/auth-error';
import { RegionalDateService } from '../../core/services/regional-date.service';
import { ConfirmationDialogService } from '../../shared/confirmation-dialog/confirmation-dialog.service';
import { NoteApiService } from '../notes/data/note-api.service';
import { NoteRequest, NoteResponse } from '../notes/data/note.models';

function localDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function nonBlank(control: AbstractControl): ValidationErrors | null {
  return typeof control.value === 'string' && !control.value.trim() ? { required: true } : null;
}

@Component({
  selector: 'app-journal-page',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './journal.page.html',
  styleUrl: './journal.page.css',
})
export class JournalPage {
  private readonly formBuilder = inject(FormBuilder);
  private readonly notesApi = inject(NoteApiService);
  private readonly confirmation = inject(ConfirmationDialogService);
  private readonly regionalDate = inject(RegionalDateService);
  private dayRequestId = 0;

  readonly selectedDate = signal(localDate(new Date()));
  readonly entry = signal<NoteResponse | null>(null);
  readonly history = signal<NoteResponse[]>([]);
  readonly dayLoading = signal(true);
  readonly historyLoading = signal(true);
  readonly saving = signal(false);
  readonly deletingId = signal<number | null>(null);
  readonly dayError = signal<string | null>(null);
  readonly historyError = signal<string | null>(null);
  readonly saveError = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly historySearch = signal('');
  readonly historyDate = signal('');

  readonly form = this.formBuilder.nonNullable.group({
    title: ['', [Validators.required, nonBlank, Validators.maxLength(200)]],
    content: ['', [Validators.required, nonBlank, Validators.maxLength(50000)]],
  });

  readonly journalEntries = computed(() => {
    const search = this.historySearch().trim().toLowerCase();
    const date = this.historyDate();
    return this.history().filter(
      (note) =>
        note.noteType === 'JOURNAL' &&
        (!date || note.journalDate === date) &&
        (!search ||
          note.title.toLowerCase().includes(search) ||
          note.content.toLowerCase().includes(search)),
    );
  });

  constructor() {
    this.loadJournalDay(this.selectedDate());
    this.loadHistory();
  }

  loadJournalDay(date: string): void {
    const requestId = ++this.dayRequestId;
    this.dayLoading.set(true);
    this.dayError.set(null);
    this.saveError.set(null);
    this.entry.set(null);
    this.form.reset({ title: '', content: '' });
    this.notesApi
      .journal(date)
      .pipe(
        finalize(() => {
          if (requestId === this.dayRequestId) this.dayLoading.set(false);
        }),
      )
      .subscribe({
        next: (entries) => {
          if (requestId !== this.dayRequestId) return;
          const entry = entries[0] ?? null;
          this.entry.set(entry);
          this.form.reset({ title: entry?.title ?? '', content: entry?.content ?? '' });
        },
        error: (error: unknown) => {
          if (requestId !== this.dayRequestId) return;
          this.dayError.set(readApiError(error, 'Could not load this journal page.'));
        },
      });
  }

  loadHistory(): void {
    this.historyLoading.set(true);
    this.historyError.set(null);
    this.notesApi
      .listAll({ sortBy: 'journalDate', direction: 'DESC' })
      .pipe(finalize(() => this.historyLoading.set(false)))
      .subscribe({
        next: (notes) => this.history.set(notes.filter((note) => note.noteType === 'JOURNAL')),
        error: (error: unknown) => {
          this.historyError.set(readApiError(error, 'Could not load your journal history.'));
        },
      });
  }

  async chooseDate(date: string): Promise<void> {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(date) || date === this.selectedDate()) return;
    if (!(await this.confirmDiscardDraft())) return;
    this.selectedDate.set(date);
    this.loadJournalDay(date);
  }

  async moveDate(offset: number): Promise<void> {
    if (!(await this.confirmDiscardDraft())) return;
    const date = new Date(`${this.selectedDate()}T00:00:00`);
    date.setDate(date.getDate() + offset);
    const next = localDate(date);
    this.selectedDate.set(next);
    this.loadJournalDay(next);
  }

  async openHistoryEntry(note: NoteResponse): Promise<void> {
    if (!note.journalDate) return;
    if (note.journalDate === this.selectedDate() && this.entry()?.id === note.id) {
      this.form.reset({ title: note.title, content: note.content });
      return;
    }
    await this.chooseDate(note.journalDate);
  }

  saveEntry(): void {
    if (this.dayLoading() || this.dayError() || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.saveError.set(null);
    const values = this.form.getRawValue();
    const request: NoteRequest = {
      title: values.title.trim(),
      content: values.content.trim(),
      noteType: 'JOURNAL',
      journalDate: this.selectedDate(),
    };
    const current = this.entry();
    const operation = current
      ? this.notesApi.update(current.id, request)
      : this.notesApi.create(request);
    operation.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (saved) => {
        this.entry.set(saved);
        this.form.reset({ title: saved.title, content: saved.content });
        this.success.set('Your words have been tucked safely into the garden.');
        this.loadHistory();
      },
      error: (error: unknown) => {
        this.saveError.set(
          readApiError(
            error,
            'Could not save this entry. A journal page may already exist for this date.',
          ),
        );
      },
    });
  }

  async resetEntry(): Promise<void> {
    if (!(await this.confirmDiscardDraft())) return;
    const entry = this.entry();
    this.form.reset({ title: entry?.title ?? '', content: entry?.content ?? '' });
    this.saveError.set(null);
  }

  async deleteEntry(note: NoteResponse): Promise<void> {
    if (!(await this.confirmation.confirm({
      title: 'Delete this journal entry?',
      message: `Delete the journal entry for ${note.journalDate}? This cannot be undone.`,
      confirmLabel: 'Yes, Delete',
    }))) return;
    this.deletingId.set(note.id);
    this.notesApi
      .delete(note.id)
      .pipe(finalize(() => this.deletingId.set(null)))
      .subscribe({
        next: () => {
          this.success.set('That page has been removed from your memory garden.');
          if (this.entry()?.id === note.id) this.loadJournalDay(this.selectedDate());
          this.loadHistory();
        },
        error: (error: unknown) => {
          this.historyError.set(readApiError(error, 'Could not delete this journal entry.'));
        },
      });
  }

  formatDate(date: string): string {
    return this.regionalDate.formatDateOnly(date, {
      weekday: 'long',
      month: 'long',
      day: 'numeric',
      year: 'numeric',
    });
  }

  formatShortDate(date: string | null): string {
    if (!date) return 'Undated';
    return this.regionalDate.formatDateOnly(date, {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  }

  clearSuccess(): void {
    this.success.set(null);
  }

  private confirmDiscardDraft(): Promise<boolean> {
    if (!this.form.dirty) return Promise.resolve(true);
    return this.confirmation.confirm({
      title: 'Discard your changes?',
      message: 'Leave this page and discard your unsaved changes?',
      confirmLabel: 'Yes, Discard',
    });
  }
}

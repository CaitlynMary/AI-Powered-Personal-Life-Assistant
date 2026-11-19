import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { readApiError } from '../../core/auth/auth-error';
import { RegionalDateService } from '../../core/services/regional-date.service';
import { ConfirmationDialogService } from '../../shared/confirmation-dialog/confirmation-dialog.service';
import { NoteApiService } from './data/note-api.service';
import { NoteQuery, NoteRequest, NoteResponse, NoteSortField, NoteType } from './data/note.models';
import { NoteEditor, NoteEditorMode } from './note-editor/note-editor';

const NOTES_PER_PAGE = 12;

@Component({
  selector: 'app-notes-page',
  standalone: true,
  imports: [CommonModule, FormsModule, NoteEditor],
  templateUrl: './notes.page.html',
})
export class NotesPage {
  private readonly notesApi = inject(NoteApiService);
  private readonly confirmation = inject(ConfirmationDialogService);
  private readonly regionalDate = inject(RegionalDateService);
  private requestId = 0;

  readonly allNotes = signal<NoteResponse[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly submittedSearch = signal('');
  readonly searchText = signal('');
  readonly noteTypeFilter = signal<NoteType | ''>('');
  readonly sortBy = signal<NoteSortField>('updatedAt');
  readonly direction = signal<'ASC' | 'DESC'>('DESC');
  readonly gridView = signal(true);
  readonly page = signal(0);
  readonly editorOpen = signal(false);
  readonly editorMode = signal<NoteEditorMode>('CREATE');
  readonly editorNote = signal<NoteResponse | null>(null);
  readonly editorError = signal<string | null>(null);
  readonly saving = signal(false);
  readonly deletingId = signal<number | null>(null);
  readonly pinningId = signal<number | null>(null);

  readonly filteredNotes = computed(() => {
    const type = this.noteTypeFilter();
    return this.allNotes().filter((note) => !type || note.noteType === type);
  });
  readonly pageCount = computed(() =>
    Math.max(1, Math.ceil(this.filteredNotes().length / NOTES_PER_PAGE)),
  );
  readonly visibleNotes = computed(() =>
    this.filteredNotes().slice(this.page() * NOTES_PER_PAGE, (this.page() + 1) * NOTES_PER_PAGE),
  );

  constructor() {
    this.loadNotes();
  }

  loadNotes(): void {
    const requestId = ++this.requestId;
    this.loading.set(true);
    this.error.set(null);
    const query: Omit<NoteQuery, 'page' | 'size'> = {
      search: this.submittedSearch(),
      sortBy: this.sortBy(),
      direction: this.direction(),
    };
    this.notesApi
      .listAll(query)
      .pipe(
        finalize(() => {
          if (requestId === this.requestId) this.loading.set(false);
        }),
      )
      .subscribe({
        next: (notes) => {
          if (requestId !== this.requestId) return;
          this.allNotes.set(notes);
          if (this.page() >= Math.max(1, Math.ceil(this.filteredNotes().length / NOTES_PER_PAGE))) {
            this.page.set(0);
          }
        },
        error: (error: unknown) => {
          if (requestId !== this.requestId) return;
          this.error.set(readApiError(error, 'Could not load your notes. Please try again.'));
        },
      });
  }

  applySearch(): void {
    this.submittedSearch.set(this.searchText().trim());
    this.page.set(0);
    this.loadNotes();
  }

  setTypeFilter(value: string): void {
    if (['', 'GENERAL', 'JOURNAL'].includes(value)) {
      this.noteTypeFilter.set(value as NoteType | '');
      this.page.set(0);
    }
  }

  setSortBy(value: string): void {
    const allowed: NoteSortField[] = [
      'createdAt',
      'updatedAt',
      'title',
      'noteType',
      'pinned',
      'journalDate',
    ];
    if (!allowed.includes(value as NoteSortField)) return;
    this.sortBy.set(value as NoteSortField);
    this.page.set(0);
    this.loadNotes();
  }

  beginCreate(): void {
    this.editorMode.set('CREATE');
    this.editorNote.set(null);
    this.editorError.set(null);
    this.editorOpen.set(true);
  }

  viewNote(note: NoteResponse): void {
    this.openEditor(note, 'VIEW');
  }

  editNote(note: NoteResponse): void {
    this.openEditor(note, 'EDIT');
  }

  private openEditor(note: NoteResponse | null, mode: NoteEditorMode): void {
    this.editorNote.set(note);
    this.editorMode.set(mode);
    this.editorError.set(null);
    this.editorOpen.set(true);
  }

  saveNote(event: { id: number | null; request: NoteRequest }): void {
    this.saving.set(true);
    this.editorError.set(null);
    const operation =
      event.id === null
        ? this.notesApi.create(event.request)
        : this.notesApi.update(event.id, event.request);
    operation.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.editorOpen.set(false);
        this.success.set(
          event.id === null ? 'Your note has been saved.' : 'Your note was updated.',
        );
        this.loadNotes();
      },
      error: (error: unknown) => {
        this.editorError.set(readApiError(error, 'Could not save your note. Please try again.'));
      },
    });
  }

  togglePin(note: NoteResponse): void {
    this.pinningId.set(note.id);
    this.actionError.set(null);
    this.notesApi
      .togglePin(note.id)
      .pipe(finalize(() => this.pinningId.set(null)))
      .subscribe({
        next: () => {
          this.success.set(note.pinned ? 'Note unpinned.' : 'Note pinned.');
          this.loadNotes();
        },
        error: (error: unknown) => {
          this.actionError.set(readApiError(error, 'Could not update the note pin.'));
        },
      });
  }

  async deleteNote(note: NoteResponse): Promise<void> {
    if (!(await this.confirmation.confirm({
      title: 'Delete this note?',
      message: `Delete "${note.title}"? This cannot be undone.`,
      confirmLabel: 'Yes, Delete',
    }))) return;
    this.deletingId.set(note.id);
    this.actionError.set(null);
    this.notesApi
      .delete(note.id)
      .pipe(finalize(() => this.deletingId.set(null)))
      .subscribe({
        next: () => {
          this.success.set('Note deleted.');
          this.loadNotes();
        },
        error: (error: unknown) => {
          this.actionError.set(readApiError(error, 'Could not delete this note.'));
        },
      });
  }

  changePage(offset: number): void {
    const next = this.page() + offset;
    if (next < 0 || next >= this.pageCount()) return;
    this.page.set(next);
  }

  noteDate(note: NoteResponse): string {
    const updated = note.updatedAt !== note.createdAt;
    const value = updated ? note.updatedAt : note.createdAt;
    return `${updated ? 'Updated' : 'Created'} ${this.regionalDate.formatDateTime(value, {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    })}`;
  }

  clearMessages(): void {
    this.success.set(null);
    this.actionError.set(null);
  }
}

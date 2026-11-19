import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../core/api.config';
import { ConfirmationDialogService } from '../../shared/confirmation-dialog/confirmation-dialog.service';
import { NotePageResponse, NoteResponse } from './data/note.models';
import { NotesPage } from './notes.page';

function makeNote(overrides: Partial<NoteResponse> = {}): NoteResponse {
  return {
    id: 1,
    title: 'A quiet thought',
    content: 'Remember the little things.',
    noteType: 'GENERAL',
    pinned: false,
    journalDate: null,
    createdAt: '2026-11-12T09:00:00',
    updatedAt: '2026-11-12T09:00:00',
    ...overrides,
  };
}

function pageResponse(content: NoteResponse[]): NotePageResponse {
  return {
    content,
    page: 0,
    size: 100,
    totalElements: content.length,
    totalPages: content.length ? 1 : 0,
    last: true,
  };
}

describe('NotesPage', () => {
  let fixture: ComponentFixture<NotesPage>;
  let component: NotesPage;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    fixture = TestBed.createComponent(NotesPage);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify({ ignoreCancelled: true }));

  function flushList(notes: NoteResponse[]): void {
    fixture.detectChanges();
    const request = http.expectOne((req) => req.url === `${API_BASE_URL}/api/notes`);
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('100');
    request.flush(pageResponse(notes));
    fixture.detectChanges();
  }

  it('loads notes, shows type filters, and filters the displayed notes', () => {
    flushList([
      makeNote(),
      makeNote({
        id: 2,
        title: 'River evening',
        noteType: 'JOURNAL',
        journalDate: '2026-11-11',
      }),
    ]);
    expect(fixture.nativeElement.textContent).toContain('My Little Notes');
    expect(fixture.nativeElement.textContent).toContain('A quiet thought');
    expect(fixture.nativeElement.textContent).toContain('River evening');

    component.setTypeFilter('JOURNAL');
    fixture.detectChanges();
    expect(component.visibleNotes().map((note) => note.title)).toEqual(['River evening']);
    expect(fixture.nativeElement.textContent).not.toContain('A quiet thought');

    component.setTypeFilter('');
    component.gridView.set(false);
    fixture.detectChanges();
    expect(component.visibleNotes()).toHaveLength(2);
  });

  it('searches through the backend and creates a note before refreshing the list', () => {
    flushList([]);
    component.searchText.set('  violet sky  ');
    component.applySearch();
    const search = http.expectOne((req) => req.url === `${API_BASE_URL}/api/notes`);
    expect(search.request.params.get('search')).toBe('violet sky');
    search.flush(pageResponse([]));

    component.saveNote({
      id: null,
      request: {
        title: 'Small joy',
        content: 'A warm cup of tea.',
        noteType: 'GENERAL',
        journalDate: null,
      },
    });
    const create = http.expectOne(`${API_BASE_URL}/api/notes`);
    expect(create.request.method).toBe('POST');
    expect(create.request.body.noteType).toBe('GENERAL');
    expect(create.request.body.journalDate).toBeNull();
    create.flush(makeNote({ id: 5, title: 'Small joy' }));
    expect(component.success()).toBe('Your note has been saved.');
    const refreshed = http.expectOne((req) => req.url === `${API_BASE_URL}/api/notes`);
    expect(refreshed.request.params.get('search')).toBe('violet sky');
    refreshed.flush(pageResponse([makeNote({ id: 5, title: 'Small joy' })]));
  });

  it('edits, pins, and deletes notes through backend-confirmed operations', async () => {
    const note = makeNote();
    flushList([note]);
    component.editNote(note);
    component.saveNote({
      id: note.id,
      request: {
        title: 'Updated thought',
        content: note.content,
        noteType: 'GENERAL',
        journalDate: null,
      },
    });
    const update = http.expectOne(`${API_BASE_URL}/api/notes/1`);
    expect(update.request.method).toBe('PUT');
    update.flush(makeNote({ title: 'Updated thought' }));
    http.expectOne((req) => req.url === `${API_BASE_URL}/api/notes`).flush(pageResponse([]));

    component.togglePin(note);
    const pin = http.expectOne(`${API_BASE_URL}/api/notes/1/pin`);
    expect(pin.request.method).toBe('PATCH');
    pin.flush(makeNote({ pinned: true }));
    http.expectOne((req) => req.url === `${API_BASE_URL}/api/notes`).flush(pageResponse([]));

    const confirm = vi
      .spyOn(TestBed.inject(ConfirmationDialogService), 'confirm')
      .mockResolvedValue(true);
    await component.deleteNote(note);
    const deletion = http.expectOne(`${API_BASE_URL}/api/notes/1`);
    expect(deletion.request.method).toBe('DELETE');
    deletion.flush(null);
    http.expectOne((req) => req.url === `${API_BASE_URL}/api/notes`).flush(pageResponse([]));
    expect(component.success()).toBe('Note deleted.');
    confirm.mockRestore();
  });

  it('shows loading, empty, and recoverable error states', async () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Opening your little notebook');
    http.expectOne((req) => req.url === `${API_BASE_URL}/api/notes`).flush(pageResponse([]));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Your notebook is waiting');

    component.loadNotes();
    http
      .expectOne((req) => req.url === `${API_BASE_URL}/api/notes`)
      .flush({ message: 'Notes unavailable' }, { status: 503, statusText: 'Unavailable' });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Notes unavailable');
    expect(fixture.nativeElement.textContent).toContain('Try again');
  });
});

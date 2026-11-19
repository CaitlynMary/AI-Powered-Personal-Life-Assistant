import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../core/api.config';
import { ConfirmationDialogService } from '../../shared/confirmation-dialog/confirmation-dialog.service';
import { NotePageResponse, NoteResponse } from '../notes/data/note.models';
import { JournalPage } from './journal.page';

function journalNote(
  id: number,
  date: string,
  overrides: Partial<NoteResponse> = {},
): NoteResponse {
  return {
    id,
    title: `Entry ${date}`,
    content: `A memory from ${date}.`,
    noteType: 'JOURNAL',
    journalDate: date,
    pinned: false,
    createdAt: `${date}T20:00:00`,
    updatedAt: `${date}T20:00:00`,
    ...overrides,
  };
}

function notePage(content: NoteResponse[]): NotePageResponse {
  return {
    content,
    page: 0,
    size: 100,
    totalElements: content.length,
    totalPages: content.length ? 1 : 0,
    last: true,
  };
}

describe('JournalPage', () => {
  let fixture: ComponentFixture<JournalPage>;
  let component: JournalPage;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    fixture = TestBed.createComponent(JournalPage);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify({ ignoreCancelled: true }));

  function flushInitial(dayEntries: NoteResponse[], history: NoteResponse[]): void {
    fixture.detectChanges();
    const date = component.selectedDate();
    const day = http.expectOne(
      (request) =>
        request.url === `${API_BASE_URL}/api/notes/journal` && request.params.get('date') === date,
    );
    day.flush(dayEntries);
    const list = http.expectOne((request) => request.url === `${API_BASE_URL}/api/notes`);
    expect(list.request.params.get('sortBy')).toBe('journalDate');
    expect(list.request.params.get('direction')).toBe('DESC');
    list.flush(notePage(history));
    fixture.detectChanges();
  }

  it('loads today’s journal page and displays history separately from general notes', () => {
    const todayEntry = journalNote(1, component.selectedDate(), { title: 'A gentle day' });
    const older = journalNote(2, '2026-11-10', { title: 'River evening' });
    flushInitial(
      [todayEntry],
      [todayEntry, older, { ...older, id: 3, noteType: 'GENERAL', journalDate: null }],
    );

    expect(fixture.nativeElement.textContent).toContain('Dear, little me');
    expect(fixture.nativeElement.textContent).toContain('My Memory Garden');
    const heroImage = fixture.nativeElement.querySelector(
      '.journal-image-frame img',
    ) as HTMLImageElement;
    expect(heroImage.getAttribute('src')).toBe('/images/journal-desk.png');
    expect(heroImage.alt).toContain('writing in her journal');
    expect(component.entry()?.id).toBe(1);
    expect(component.journalEntries().map((note) => note.id)).toEqual([1, 2]);
    expect(fixture.nativeElement.textContent).toContain('A gentle day');
    expect(fixture.nativeElement.textContent).not.toContain('Opening your page');

    component.historySearch.set('river');
    expect(component.journalEntries().map((note) => note.id)).toEqual([2]);
    component.historySearch.set('  VER EVEN  ');
    expect(component.journalEntries().map((note) => note.id)).toEqual([2]);
    component.historySearch.set('MEMORY FROM');
    expect(component.journalEntries().map((note) => note.id)).toEqual([1, 2]);
    component.historySearch.set('no-such-entry');
    expect(component.journalEntries()).toEqual([]);
    component.historySearch.set('');
    expect(component.journalEntries()).toHaveLength(2);
    component.historyDate.set('2026-11-10');
    expect(component.journalEntries().map((note) => note.id)).toEqual([2]);
  });

  it('validates journal fields and creates a date-unique entry before refreshing day and history', () => {
    flushInitial([], []);
    component.form.setValue({ title: '   ', content: 'A thought' });
    component.saveEntry();
    expect(component.form.invalid).toBe(true);
    http.expectNone(`${API_BASE_URL}/api/notes`);

    component.form.setValue({ title: 'A small joy', content: 'The sunset was lovely.' });
    component.saveEntry();
    const create = http.expectOne(`${API_BASE_URL}/api/notes`);
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual({
      title: 'A small joy',
      content: 'The sunset was lovely.',
      noteType: 'JOURNAL',
      journalDate: component.selectedDate(),
    });
    const saved = journalNote(7, component.selectedDate(), {
      title: 'A small joy',
      content: 'The sunset was lovely.',
    });
    create.flush(saved);
    expect(component.success()).toContain('tucked safely');

    http
      .expectOne((request) => request.url === `${API_BASE_URL}/api/notes`)
      .flush(notePage([saved]));
    expect(component.entry()?.id).toBe(7);
    expect(component.form.pristine).toBe(true);
  });

  it('opens a past history entry for editing and updates it with its original journal date', async () => {
    const older = journalNote(2, '2026-11-10', { title: 'River evening' });
    flushInitial([], [older]);

    component.form.setValue({ title: 'Unsaved', content: 'Draft' });
    const confirm = vi
      .spyOn(TestBed.inject(ConfirmationDialogService), 'confirm')
      .mockResolvedValue(true);
    await component.openHistoryEntry(older);
    expect(component.selectedDate()).toBe('2026-11-10');
    const day = http.expectOne(
      (request) =>
        request.url === `${API_BASE_URL}/api/notes/journal` &&
        request.params.get('date') === '2026-11-10',
    );
    day.flush([older]);
    expect(component.form.controls.title.value).toBe('River evening');

    component.form.setValue({ title: 'River at dusk', content: 'A longer memory.' });
    component.saveEntry();
    const update = http.expectOne(`${API_BASE_URL}/api/notes/2`);
    expect(update.request.method).toBe('PUT');
    expect(update.request.body.journalDate).toBe('2026-11-10');
    const updated = journalNote(2, '2026-11-10', {
      title: 'River at dusk',
      content: 'A longer memory.',
    });
    update.flush(updated);
    http
      .expectOne((request) => request.url === `${API_BASE_URL}/api/notes`)
      .flush(notePage([updated]));
    expect(component.entry()?.title).toBe('River at dusk');
    confirm.mockRestore();
  });

  it('shows loading, empty, and API error states and deletes only after confirmation', async () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Opening your page');
    const day = http.expectOne((request) => request.url === `${API_BASE_URL}/api/notes/journal`);
    day.flush([]);
    http.expectOne((request) => request.url === `${API_BASE_URL}/api/notes`).flush(notePage([]));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Your memory garden is ready to grow.');

    component.loadJournalDay(component.selectedDate());
    http
      .expectOne((request) => request.url === `${API_BASE_URL}/api/notes/journal`)
      .flush({ message: 'Journal unavailable' }, { status: 503, statusText: 'Unavailable' });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Journal unavailable');

    const note = journalNote(9, '2026-11-09');
    const confirm = vi
      .spyOn(TestBed.inject(ConfirmationDialogService), 'confirm')
      .mockResolvedValue(true);
    await component.deleteEntry(note);
    const deletion = http.expectOne(`${API_BASE_URL}/api/notes/9`);
    expect(deletion.request.method).toBe('DELETE');
    deletion.flush(null);
    http.expectOne((request) => request.url === `${API_BASE_URL}/api/notes`).flush(notePage([]));
    expect(component.success()).toContain('removed from your memory garden');
    confirm.mockRestore();
  });
});

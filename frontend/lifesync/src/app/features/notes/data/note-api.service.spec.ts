import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/api.config';
import { NoteApiService } from './note-api.service';

describe('NoteApiService', () => {
  let service: NoteApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(NoteApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('uses the supported list, search, sort, journal-date, and statistics endpoints', () => {
    service
      .list({ page: 2, size: 12, search: '  violet sky ', sortBy: 'journalDate', direction: 'ASC' })
      .subscribe();
    const list = http.expectOne((request) => request.url === `${API_BASE_URL}/api/notes`);
    expect(list.request.method).toBe('GET');
    expect(list.request.params.get('page')).toBe('2');
    expect(list.request.params.get('size')).toBe('12');
    expect(list.request.params.get('search')).toBe('violet sky');
    expect(list.request.params.get('sortBy')).toBe('journalDate');
    expect(list.request.params.get('direction')).toBe('ASC');
    list.flush({ content: [], page: 2, size: 12, totalElements: 0, totalPages: 0, last: true });

    service.journal('2026-11-12').subscribe();
    const journal = http.expectOne(
      (request) => request.url === `${API_BASE_URL}/api/notes/journal`,
    );
    expect(journal.request.params.get('date')).toBe('2026-11-12');
    journal.flush([]);

    service.statistics().subscribe();
    const statistics = http.expectOne(`${API_BASE_URL}/api/notes/statistics`);
    statistics.flush({ totalNotes: 5, generalNotes: 3, journalEntries: 2, pinnedNotes: 1 });
  });

  it('loads all note pages sequentially using the valid maximum page size', () => {
    const result: unknown[] = [];
    service
      .listAll({ sortBy: 'updatedAt', direction: 'DESC' })
      .subscribe((notes) => result.push(...notes));
    const first = http.expectOne((request) => request.url === `${API_BASE_URL}/api/notes`);
    expect(first.request.params.get('page')).toBe('0');
    expect(first.request.params.get('size')).toBe('100');
    first.flush({
      content: [{ id: 1, title: 'First' }],
      page: 0,
      size: 100,
      totalElements: 101,
      totalPages: 2,
      last: false,
    });

    const second = http.expectOne(
      (request) =>
        request.url === `${API_BASE_URL}/api/notes` && request.params.get('page') === '1',
    );
    second.flush({
      content: [{ id: 2, title: 'Second' }],
      page: 1,
      size: 100,
      totalElements: 101,
      totalPages: 2,
      last: true,
    });
    expect(result).toEqual([
      { id: 1, title: 'First' },
      { id: 2, title: 'Second' },
    ]);
  });

  it('creates, reads, updates, pins, and deletes notes using backend-supported fields', () => {
    const body = {
      title: 'A tiny thought',
      content: 'Keep looking for small joys.',
      noteType: 'GENERAL' as const,
      journalDate: null,
    };

    service.create(body).subscribe();
    const create = http.expectOne(`${API_BASE_URL}/api/notes`);
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual(body);
    create.flush({
      id: 3,
      ...body,
      pinned: false,
      createdAt: '2026-11-12T10:00:00',
      updatedAt: '2026-11-12T10:00:00',
    });

    service.get(3).subscribe();
    const get = http.expectOne(`${API_BASE_URL}/api/notes/3`);
    expect(get.request.method).toBe('GET');
    get.flush({ id: 3, ...body, pinned: false });

    service.update(3, body).subscribe();
    const update = http.expectOne(`${API_BASE_URL}/api/notes/3`);
    expect(update.request.method).toBe('PUT');
    expect(update.request.body).toEqual(body);
    update.flush({ id: 3, ...body, pinned: false });

    service.togglePin(3).subscribe();
    const pin = http.expectOne(`${API_BASE_URL}/api/notes/3/pin`);
    expect(pin.request.method).toBe('PATCH');
    expect(pin.request.body).toBeNull();
    pin.flush({ id: 3, ...body, pinned: true });

    service.delete(3).subscribe();
    const deletion = http.expectOne(`${API_BASE_URL}/api/notes/3`);
    expect(deletion.request.method).toBe('DELETE');
    deletion.flush(null);
  });

  it('propagates backend errors to the caller', () => {
    let receivedError: unknown;
    service.journal('2026-11-12').subscribe({
      error: (error: unknown) => (receivedError = error),
    });
    http
      .expectOne(`${API_BASE_URL}/api/notes/journal?date=2026-11-12`)
      .flush({ message: 'Unavailable' }, { status: 503, statusText: 'Unavailable' });
    expect(receivedError).toBeTruthy();
  });
});

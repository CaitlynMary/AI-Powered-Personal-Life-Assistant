import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../core/api.config';
import { ConfirmationDialogService } from '../../shared/confirmation-dialog/confirmation-dialog.service';
import { ReminderResponse } from './data/reminder.models';
import { RemindersPage } from './reminders.page';

function makeReminder(overrides: Partial<ReminderResponse> = {}): ReminderResponse {
  return {
    id: 1,
    title: 'Call the dentist',
    message: 'Ask about the next appointment.',
    reminderDateTime: '2026-11-18T10:30:00',
    reminderType: 'GENERAL',
    status: 'PENDING',
    createdAt: '2026-11-12T09:00:00',
    updatedAt: '2026-11-12T09:00:00',
    ...overrides,
  };
}

function pageResponse(content: ReminderResponse[]) {
  return {
    content,
    page: 0,
    size: 100,
    totalElements: content.length,
    totalPages: content.length ? 1 : 0,
    last: true,
  };
}

describe('RemindersPage', () => {
  let fixture: ComponentFixture<RemindersPage>;
  let component: RemindersPage;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    fixture = TestBed.createComponent(RemindersPage);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify({ ignoreCancelled: true }));

  function flushList(reminders: ReminderResponse[]): void {
    fixture.detectChanges();
    const request = http.expectOne(
      (req) => req.url === `${API_BASE_URL}/api/reminders` && req.params.get('page') === '0',
    );
    expect(request.request.params.get('size')).toBe('100');
    request.flush(pageResponse(reminders));
    fixture.detectChanges();
  }

  it('renders reminders, derives status totals, and searches and filters the list', () => {
    flushList([
      makeReminder(),
      makeReminder({
        id: 2,
        title: 'Stretch break',
        message: 'Stretch your shoulders.',
        reminderType: 'HABIT',
        status: 'SENT',
        reminderDateTime: '2026-11-17T09:00:00',
      }),
    ]);

    expect(fixture.nativeElement.textContent).toContain('My Reminders');
    expect(fixture.nativeElement.textContent).toContain('Call the dentist');
    expect(component.summary().total).toBe(2);
    expect(component.summary().sent).toBe(1);

    component.searchText.set('  STRETCH  ');
    fixture.detectChanges();
    expect(component.visibleReminders().map((item) => item.id)).toEqual([2]);
    component.searchText.set('APPOINTMENT');
    expect(component.visibleReminders().map((item) => item.id)).toEqual([1]);
    component.searchText.set('no-such-reminder');
    expect(component.visibleReminders()).toEqual([]);
    component.searchText.set('');
    expect(component.visibleReminders()).toHaveLength(2);
    component.setStatusFilter('PENDING');
    expect(component.visibleReminders().map((item) => item.id)).toEqual([1]);
  });

  it('creates and updates reminders, then reloads the actual API list', () => {
    flushList([]);
    component.saveReminder({
      id: null,
      request: {
        title: 'Call the dentist',
        message: 'Ask about the next appointment.',
        reminderDateTime: '2026-11-18T10:30:00',
        reminderType: 'GENERAL',
      },
    });
    const create = http.expectOne(`${API_BASE_URL}/api/reminders`);
    expect(create.request.method).toBe('POST');
    create.flush(makeReminder());
    http
      .expectOne((req) => req.url === `${API_BASE_URL}/api/reminders`)
      .flush(pageResponse([makeReminder()]));

    component.saveReminder({
      id: 1,
      request: {
        title: 'Dentist appointment',
        message: 'Bring the paperwork.',
        reminderDateTime: '2026-11-18T11:00:00',
        reminderType: 'GENERAL',
      },
    });
    const update = http.expectOne(`${API_BASE_URL}/api/reminders/1`);
    expect(update.request.method).toBe('PUT');
    update.flush(makeReminder({ title: 'Dentist appointment' }));
    http.expectOne((req) => req.url === `${API_BASE_URL}/api/reminders`).flush(pageResponse([]));
    expect(component.success()).toBe('Reminder updated.');
  });

  it('cancels a pending reminder only after confirmation and refreshes the list', async () => {
    flushList([makeReminder()]);
    const confirm = vi
      .spyOn(TestBed.inject(ConfirmationDialogService), 'confirm')
      .mockResolvedValue(true);

    await component.cancelReminder(makeReminder());

    const deletion = http.expectOne(`${API_BASE_URL}/api/reminders/1`);
    expect(deletion.request.method).toBe('DELETE');
    deletion.flush(null);
    http.expectOne((req) => req.url === `${API_BASE_URL}/api/reminders`).flush(pageResponse([]));
    expect(component.success()).toBe('Reminder cancelled.');
    confirm.mockRestore();
  });

  it('shows empty and recoverable error states', () => {
    flushList([]);
    expect(fixture.nativeElement.textContent).toContain('A little more breathing room');

    component.loadReminders();
    http
      .expectOne((req) => req.url === `${API_BASE_URL}/api/reminders`)
      .flush({ message: 'Reminders unavailable' }, { status: 503, statusText: 'Unavailable' });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Reminders unavailable');
    expect(fixture.nativeElement.textContent).toContain('Retry');
  });
});

import { TestBed } from '@angular/core/testing';
import { CalendarEventResponse } from '../data/calendar.models';
import { CalendarEventForm } from './event-form';

describe('CalendarEventForm', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('rejects missing titles and invalid start/end ordering', () => {
    const fixture = TestBed.createComponent(CalendarEventForm);
    const component = fixture.componentInstance;
    const save = vi.fn();
    component.save.subscribe(save);

    component.submit();
    expect(component.form.invalid).toBe(true);
    expect(component.form.controls.title.touched).toBe(true);
    expect(save).not.toHaveBeenCalled();

    component.form.setValue({
      title: 'Planning',
      description: '',
      eventType: 'WORK',
      allDay: false,
      startDateTime: '2026-11-11T10:00',
      endDateTime: '2026-11-11T09:00',
      recurrenceType: 'NONE',
      taskId: '',
    });
    expect(component.form.hasError('invalidInterval')).toBe(true);
    component.submit();
    expect(save).not.toHaveBeenCalled();
  });

  it('submits the verified event fields and preserves values in edit mode', () => {
    const fixture = TestBed.createComponent(CalendarEventForm);
    const component = fixture.componentInstance;
    const save = vi.fn();
    component.save.subscribe(save);
    component.form.setValue({
      title: '  Planning  ',
      description: '  Weekly review  ',
      eventType: 'WORK',
      allDay: false,
      startDateTime: '2026-11-11T09:00',
      endDateTime: '2026-11-11T10:00',
      recurrenceType: 'WEEKLY',
      taskId: '7',
    });
    component.submit();
    expect(save).toHaveBeenCalledWith({
      id: null,
      request: {
        title: 'Planning',
        description: 'Weekly review',
        eventType: 'WORK',
        allDay: false,
        startDateTime: '2026-11-11T09:00:00',
        endDateTime: '2026-11-11T10:00:00',
        recurrenceType: 'WEEKLY',
        taskId: 7,
      },
    });

    const event: CalendarEventResponse = {
      id: 9,
      title: 'Existing meeting',
      description: 'Conference call',
      eventType: 'MEETING',
      allDay: false,
      startDateTime: '2026-11-12T13:00:00',
      endDateTime: '2026-11-12T14:00:00',
      recurrenceType: 'NONE',
      taskId: null,
      taskTitle: null,
      createdAt: '2026-11-01T10:00:00',
      updatedAt: '2026-11-01T10:00:00',
    };
    fixture.componentRef.setInput('event', event);
    fixture.detectChanges();
    expect(component.form.controls.startDateTime.value).toBe('2026-11-12T13:00');
    component.submit();
    expect(save).toHaveBeenLastCalledWith(expect.objectContaining({ id: 9 }));
  });
});

import { TestBed } from '@angular/core/testing';
import { ScheduleConflictItem } from '../data/calendar.models';
import { ScheduleForm } from './schedule-form';

describe('ScheduleForm', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('validates schedule time intervals and presents backend conflicts', () => {
    const fixture = TestBed.createComponent(ScheduleForm);
    const component = fixture.componentInstance;
    fixture.componentRef.setInput('conflicts', [
      {
        type: 'CALENDAR_EVENT',
        id: 8,
        title: 'Meeting',
        startDateTime: '2026-11-11T09:00:00',
        endDateTime: '2026-11-11T10:00:00',
      } satisfies ScheduleConflictItem,
    ]);
    fixture.detectChanges();
    const save = vi.fn();
    component.save.subscribe(save);
    component.form.setValue({
      title: 'Focus block',
      description: '',
      startDateTime: '2026-11-11T10:00',
      endDateTime: '2026-11-11T10:00',
      taskId: '',
    });
    expect(component.form.hasError('invalidInterval')).toBe(true);
    component.submit();
    expect(save).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Meeting');
    expect(fixture.nativeElement.textContent).toContain('overlaps existing items');
  });

  it('submits a schedule request with local datetime and optional task link', () => {
    const fixture = TestBed.createComponent(ScheduleForm);
    const component = fixture.componentInstance;
    const save = vi.fn();
    component.save.subscribe(save);
    component.form.setValue({
      title: '  Focus block  ',
      description: '  Deep work  ',
      startDateTime: '2026-11-11T09:00',
      endDateTime: '2026-11-11T10:00',
      taskId: '7',
    });
    component.submit();
    expect(save).toHaveBeenCalledWith({
      id: null,
      request: {
        title: 'Focus block',
        description: 'Deep work',
        startDateTime: '2026-11-11T09:00:00',
        endDateTime: '2026-11-11T10:00:00',
        taskId: 7,
      },
    });
  });
});

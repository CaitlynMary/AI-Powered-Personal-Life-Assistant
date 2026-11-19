import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { TaskApiService } from '../../tasks/data/task-api.service';
import { TaskResponse } from '../../tasks/data/task.models';
import { ReminderForm } from './reminder-form';

describe('ReminderForm', () => {
  beforeEach(() =>
    TestBed.configureTestingModule({
      providers: [{ provide: TaskApiService, useValue: { listAll: () => of([]) } }],
    }),
  );

  it('requires nonblank title and message and enforces backend length limits', () => {
    const fixture = TestBed.createComponent(ReminderForm);
    const component = fixture.componentInstance;
    const save = vi.fn();
    component.save.subscribe(save);

    component.form.setValue({
      reminderKind: 'STANDALONE',
      taskId: 0,
      reminderOffset: '1_HOUR',
      title: '   ',
      message: '   ',
      reminderDateTime: '2026-11-18T10:30',
      reminderType: 'GENERAL',
    });
    component.submit();

    expect(component.form.invalid).toBe(true);
    expect(component.form.controls.title.touched).toBe(true);
    expect(component.form.controls.message.touched).toBe(true);
    expect(save).not.toHaveBeenCalled();

    component.form.controls.title.setValue('x'.repeat(201));
    component.form.controls.message.setValue('A reminder');
    expect(component.form.controls.title.hasError('maxlength')).toBe(true);
  });

  it('submits local date-time values and reminder type without rejecting past times', () => {
    const fixture = TestBed.createComponent(ReminderForm);
    const component = fixture.componentInstance;
    const save = vi.fn();
    component.save.subscribe(save);
    component.form.setValue({
      reminderKind: 'STANDALONE',
      taskId: 0,
      reminderOffset: '1_HOUR',
      title: '  Remember this  ',
      message: '  Bring the paperwork.  ',
      reminderDateTime: '2030-01-02T03:04',
      reminderType: 'TASK',
    });

    component.submit();

    expect(save).toHaveBeenCalledWith({
      id: null,
      request: {
        title: 'Remember this',
        message: 'Bring the paperwork.',
        reminderDateTime: '2030-01-02T03:04:00',
        reminderType: 'TASK',
        taskId: null,
      },
    });
  });

  it('creates a task-linked reminder without asking for duplicate task details', () => {
    const fixture = TestBed.createComponent(ReminderForm);
    const component = fixture.componentInstance;
    const save = vi.fn();
    component.save.subscribe(save);
    component.tasks.set([
      {
        id: 33,
        title: 'Submit assignment',
        dueDate: '2030-01-02T04:04:00',
        status: 'TODO',
      } as TaskResponse,
    ]);
    component.form.setValue({
      reminderKind: 'TASK',
      taskId: 33,
      reminderOffset: '1_HOUR',
      title: '',
      message: '',
      reminderDateTime: '',
      reminderType: 'TASK',
    });
    component.onTaskChange();
    expect(component.form.controls.reminderDateTime.value).toBe('2030-01-02T03:04');

    component.submit();

    expect(save).toHaveBeenCalledWith({
      id: null,
      request: {
        title: 'Submit assignment',
        message: 'Reminder for task: Submit assignment',
        reminderDateTime: '2030-01-02T03:04:00',
        reminderType: 'TASK',
        taskId: 33,
        deadlineOffsetMinutes: 60,
      },
    });
  });
});

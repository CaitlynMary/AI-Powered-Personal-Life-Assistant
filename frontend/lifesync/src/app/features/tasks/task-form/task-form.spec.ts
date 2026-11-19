import { TestBed } from '@angular/core/testing';
import { TaskForm } from './task-form';
import { TaskResponse } from '../data/task.models';

describe('TaskForm', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('requires a title and shows validation state without emitting a request', () => {
    const fixture = TestBed.createComponent(TaskForm);
    const component = fixture.componentInstance;
    const save = vi.fn();
    component.save.subscribe(save);

    component.submit();

    expect(component.form.controls.title.touched).toBe(true);
    expect(component.form.invalid).toBe(true);
    expect(save).not.toHaveBeenCalled();
  });

  it('creates a task with supported fields and local date-time formatting', () => {
    const fixture = TestBed.createComponent(TaskForm);
    const component = fixture.componentInstance;
    const save = vi.fn();
    component.save.subscribe(save);
    component.form.setValue({
      title: '  Plan week  ',
      description: '  Prioritise projects  ',
      categoryId: '5',
      priority: 'HIGH',
      dueDate: '2026-06-15T10:30',
      startDate: '2026-06-15T09:00',
      estimatedDuration: '45',
      recurring: true,
      recurrenceType: 'WEEKLY',
    });

    component.submit();

    expect(save).toHaveBeenCalledWith({
      id: null,
      request: {
        title: 'Plan week',
        description: 'Prioritise projects',
        categoryId: 5,
        goalId: null,
        milestoneId: null,
        priority: 'HIGH',
        dueDate: '2026-06-15T10:30:00',
        startDate: '2026-06-15T09:00:00',
        estimatedDuration: 45,
        recurring: true,
        recurrenceType: 'WEEKLY',
      },
    });
  });

  it('preserves existing related IDs and values when editing', () => {
    const fixture = TestBed.createComponent(TaskForm);
    const task: TaskResponse = {
      id: 9,
      title: 'Existing task',
      description: 'Keep this description',
      categoryId: 5,
      categoryName: 'Work',
      goalId: 12,
      milestoneId: 14,
      priority: 'MEDIUM',
      status: 'IN_PROGRESS',
      dueDate: '2026-06-15T10:30:00',
      startDate: null,
      estimatedDuration: 30,
      actualDuration: null,
      completedAt: null,
      missedAt: null,
      recurring: false,
      recurrenceType: 'NONE',
      createdAt: '2026-06-01T10:00:00',
      updatedAt: '2026-06-01T10:00:00',
    };
    fixture.componentRef.setInput('task', task);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.form.controls.title.value).toBe('Existing task');
    expect(component.form.controls.categoryId.value).toBe('5');
    expect(component.form.controls.dueDate.value).toBe('2026-06-15T10:30');
    const save = vi.fn();
    component.save.subscribe(save);
    component.submit();

    expect(save).toHaveBeenCalledWith(expect.objectContaining({
      id: 9,
      request: expect.objectContaining({ goalId: 12, milestoneId: 14 }),
    }));
  });

  it('rejects invalid date order, recurring tasks without a recurrence, and non-positive durations', () => {
    const fixture = TestBed.createComponent(TaskForm);
    const component = fixture.componentInstance;
    component.form.controls.title.setValue('Valid title');
    component.form.controls.startDate.setValue('2026-06-16T10:00');
    component.form.controls.dueDate.setValue('2026-06-15T10:00');
    expect(component.form.hasError('startAfterDue')).toBe(true);

    component.form.controls.startDate.setValue('');
    component.form.controls.recurring.setValue(true);
    component.form.controls.recurrenceType.setValue('NONE');
    expect(component.form.hasError('recurrenceRequired')).toBe(true);

    component.form.controls.recurring.setValue(false);
    component.form.controls.estimatedDuration.setValue('0');
    expect(component.form.controls.estimatedDuration.hasError('min')).toBe(true);
    expect(component.form.invalid).toBe(true);
  });
});

import { TestBed } from '@angular/core/testing';
import { HabitResponse } from '../data/habit.models';
import { HabitForm } from './habit-form';

describe('HabitForm', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('validates required fields, target days, name length, and date ordering', () => {
    const fixture = TestBed.createComponent(HabitForm);
    const component = fixture.componentInstance;
    const save = vi.fn();
    component.save.subscribe(save);

    component.submit();
    expect(component.form.invalid).toBe(true);
    expect(component.form.controls.name.touched).toBe(true);
    expect(save).not.toHaveBeenCalled();

    component.form.setValue({
      name: 'Valid habit',
      description: '',
      frequency: 'WEEKLY',
      targetDaysPerWeek: 8,
      startDate: '2026-06-20',
      endDate: '2026-06-10',
    });
    expect(component.form.controls.targetDaysPerWeek.hasError('max')).toBe(true);
    expect(component.form.hasError('endBeforeStart')).toBe(true);
    component.submit();
    expect(save).not.toHaveBeenCalled();
  });

  it('submits a trimmed creation request and loads existing values when editing', () => {
    const fixture = TestBed.createComponent(HabitForm);
    const component = fixture.componentInstance;
    const save = vi.fn();
    component.save.subscribe(save);
    component.form.setValue({
      name: '  Read  ',
      description: '  Read a chapter  ',
      frequency: 'DAILY',
      targetDaysPerWeek: 5,
      startDate: '2026-06-01',
      endDate: '',
    });
    component.submit();
    expect(save).toHaveBeenCalledWith({
      id: null,
      request: {
        name: 'Read',
        description: 'Read a chapter',
        frequency: 'DAILY',
        targetDaysPerWeek: 5,
        startDate: '2026-06-01',
        endDate: null,
      },
    });

    const habit: HabitResponse = {
      id: 4,
      name: 'Existing habit',
      description: 'Existing description',
      frequency: 'WEEKLY',
      targetDaysPerWeek: 3,
      startDate: '2026-06-01',
      endDate: '2026-06-30',
      active: true,
      createdAt: '2026-06-01T10:00:00',
      updatedAt: '2026-06-02T10:00:00',
    };
    fixture.componentRef.setInput('habit', habit);
    fixture.detectChanges();
    expect(component.form.getRawValue()).toEqual({
      name: habit.name,
      description: habit.description,
      frequency: habit.frequency,
      targetDaysPerWeek: habit.targetDaysPerWeek,
      startDate: habit.startDate,
      endDate: habit.endDate,
    });
    component.submit();
    expect(save).toHaveBeenLastCalledWith(expect.objectContaining({ id: 4 }));
  });
});

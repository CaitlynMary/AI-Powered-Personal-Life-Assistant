import { TestBed } from '@angular/core/testing';
import { GoalForm } from './goal-form';
import { GoalResponse } from '../data/goal.models';

describe('GoalForm', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('validates required goal fields and date order', () => {
    const fixture = TestBed.createComponent(GoalForm);
    const component = fixture.componentInstance;
    const save = vi.fn();
    component.save.subscribe(save);

    component.submit();
    expect(component.form.invalid).toBe(true);
    expect(component.form.controls.title.touched).toBe(true);
    expect(save).not.toHaveBeenCalled();

    component.form.setValue({
      title: 'Learn Angular',
      description: '',
      goalType: 'MONTHLY',
      startDate: '2026-06-20',
      targetDate: '2026-06-10',
    });
    expect(component.form.hasError('targetBeforeStart')).toBe(true);
    component.submit();
    expect(save).not.toHaveBeenCalled();
  });

  it('submits a trimmed creation request and loads existing fields for editing', () => {
    const fixture = TestBed.createComponent(GoalForm);
    const component = fixture.componentInstance;
    const save = vi.fn();
    component.save.subscribe(save);
    component.form.setValue({
      title: '  Learn Angular  ',
      description: '  Build a project  ',
      goalType: 'MONTHLY',
      startDate: '2026-06-01',
      targetDate: '2026-06-30',
    });
    component.submit();
    expect(save).toHaveBeenCalledWith({
      id: null,
      request: {
        title: 'Learn Angular',
        description: 'Build a project',
        goalType: 'MONTHLY',
        startDate: '2026-06-01',
        targetDate: '2026-06-30',
      },
    });

    const goal: GoalResponse = {
      id: 12,
      title: 'Existing goal',
      description: 'Existing description',
      goalType: 'YEARLY',
      startDate: '2026-01-01',
      targetDate: '2026-12-31',
      status: 'IN_PROGRESS',
      progressPercentage: 50,
      createdAt: '2026-01-01T10:00:00',
      updatedAt: '2026-01-02T10:00:00',
    };
    fixture.componentRef.setInput('goal', goal);
    fixture.detectChanges();
    expect(component.form.getRawValue()).toEqual({
      title: goal.title,
      description: goal.description,
      goalType: goal.goalType,
      startDate: goal.startDate,
      targetDate: goal.targetDate,
    });
    component.submit();
    expect(save).toHaveBeenLastCalledWith(expect.objectContaining({ id: 12 }));
  });
});

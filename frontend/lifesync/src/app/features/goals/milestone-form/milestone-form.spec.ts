import { TestBed } from '@angular/core/testing';
import { MilestoneForm } from './milestone-form';
import { MilestoneResponse } from '../data/goal.models';

describe('MilestoneForm', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('requires a title and keeps the milestone target inside the goal date range', () => {
    const fixture = TestBed.createComponent(MilestoneForm);
    fixture.componentRef.setInput('goalStartDate', '2026-06-01');
    fixture.componentRef.setInput('goalTargetDate', '2026-06-30');
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const save = vi.fn();
    component.save.subscribe(save);

    component.form.controls.title.setValue('  ');
    component.form.controls.targetDate.setValue('2026-07-01');
    expect(component.form.controls.title.hasError('required')).toBe(true);
    expect(component.form.controls.targetDate.hasError('outsideGoalRange')).toBe(true);
    component.submit();
    expect(save).not.toHaveBeenCalled();
  });

  it('submits supported create and edit fields', () => {
    const fixture = TestBed.createComponent(MilestoneForm);
    fixture.componentRef.setInput('goalStartDate', '2026-06-01');
    fixture.componentRef.setInput('goalTargetDate', '2026-06-30');
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const save = vi.fn();
    component.save.subscribe(save);
    component.form.setValue({
      title: '  First step  ',
      description: '  Complete lesson one  ',
      targetDate: '2026-06-15',
    });
    component.submit();
    expect(save).toHaveBeenCalledWith({
      id: null,
      request: {
        title: 'First step',
        description: 'Complete lesson one',
        targetDate: '2026-06-15',
      },
    });

    const milestone: MilestoneResponse = {
      id: 21,
      goalId: 12,
      title: 'Saved step',
      description: 'Saved details',
      targetDate: '2026-06-20',
      status: 'IN_PROGRESS',
      completedAt: null,
      createdAt: '2026-06-01T10:00:00',
      updatedAt: '2026-06-02T10:00:00',
    };
    fixture.componentRef.setInput('milestone', milestone);
    fixture.detectChanges();
    expect(component.form.getRawValue()).toEqual({
      title: milestone.title,
      description: milestone.description,
      targetDate: milestone.targetDate,
    });
    component.submit();
    expect(save).toHaveBeenLastCalledWith(expect.objectContaining({ id: 21 }));
  });
});

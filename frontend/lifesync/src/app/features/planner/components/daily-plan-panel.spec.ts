import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/api.config';
import { DailyPlanPanel } from './daily-plan-panel';

describe('DailyPlanPanel', () => {
  let fixture: ComponentFixture<DailyPlanPanel>;
  let component: DailyPlanPanel;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    fixture = TestBed.createComponent(DailyPlanPanel);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requires an explicit valid date and availability window', () => {
    component.form.controls.dayStartTime.setValue('12:00');
    component.form.controls.dayEndTime.setValue('09:00');
    component.generatePlan();
    expect(component.form.hasError('invalidTimeWindow')).toBe(true);
    http.expectNone(`${API_BASE_URL}/api/ai/daily-plan`);
  });

  it('requests a structured draft using the user-provided availability', () => {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const date = `${tomorrow.getFullYear()}-${String(tomorrow.getMonth() + 1).padStart(2, '0')}-${String(tomorrow.getDate()).padStart(2, '0')}`;
    component.form.setValue({ date, dayStartTime: '09:00', dayEndTime: '15:00' });
    component.generatePlan();

    const request = http.expectOne(`${API_BASE_URL}/api/ai/daily-plan`);
    expect(request.request.body).toEqual({
      date,
      dayStartTime: '09:00',
      dayEndTime: '15:00',
    });
    request.flush({
      success: true,
      message: 'Draft ready',
      data: {
        date,
        summary: 'Start with the task due soonest.',
        availableWindows: [{ startTime: '09:00:00', endTime: '15:00:00' }],
        suggestions: [
          {
            taskId: 4,
            taskTitle: 'Prepare slides',
            startTime: '09:00:00',
            endTime: '10:00:00',
            priority: 'HIGH',
            rationale: 'This is a high-priority task.',
          },
        ],
        warnings: [],
      },
    });

    expect(component.plan()?.suggestions[0].taskTitle).toBe('Prepare slides');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Draft');
  });

  it('does not render a malformed backend plan as valid', () => {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const date = `${tomorrow.getFullYear()}-${String(tomorrow.getMonth() + 1).padStart(2, '0')}-${String(tomorrow.getDate()).padStart(2, '0')}`;
    component.form.setValue({ date, dayStartTime: '09:00', dayEndTime: '15:00' });
    component.generatePlan();
    http.expectOne(`${API_BASE_URL}/api/ai/daily-plan`).flush({
      success: true,
      message: 'Draft ready',
      data: {
        date,
        summary: 'Incorrect time.',
        availableWindows: [{ startTime: '09:00', endTime: '15:00' }],
        suggestions: [
          {
            taskId: 4,
            taskTitle: 'Prepare slides',
            startTime: '08:00',
            endTime: '10:00',
            priority: 'HIGH',
            rationale: 'Earlier than available.',
          },
        ],
        warnings: [],
      },
    });
    expect(component.plan()).toBeNull();
    expect(component.error()).toContain('incomplete or invalid plan');
  });

  it('keeps edited suggestions as a draft until explicit approval', () => {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const date = `${tomorrow.getFullYear()}-${String(tomorrow.getMonth() + 1).padStart(2, '0')}-${String(tomorrow.getDate()).padStart(2, '0')}`;
    component.form.setValue({ date, dayStartTime: '09:00', dayEndTime: '15:00' });
    component.generatePlan();
    http.expectOne(`${API_BASE_URL}/api/ai/daily-plan`).flush({
      success: true,
      message: 'Draft ready',
      data: {
        date,
        summary: 'A focused plan.',
        availableWindows: [{ startTime: '09:00', endTime: '15:00' }],
        suggestions: [{
          taskId: 4, taskTitle: 'Prepare slides', startTime: '09:00', endTime: '10:00',
          priority: 'HIGH', rationale: 'Urgent deadline.',
        }],
        warnings: [],
      },
    });

    component.updateSuggestion(4, 'startTime', '09:30');
    expect(component.plan()?.suggestions[0].startTime).toBe('09:30');
    http.expectNone(`${API_BASE_URL}/api/schedule/approve-daily-plan`);

    component.approvePlan();
    const approval = http.expectOne(`${API_BASE_URL}/api/schedule/approve-daily-plan`);
    expect(approval.request.body).toEqual({
      date,
      items: [{ taskId: 4, startTime: '09:30', endTime: '10:00' }],
    });
    approval.flush([]);
    expect(component.plan()).toBeNull();
    expect(component.approvalMessage()).toContain('added to your schedule');
  });
});

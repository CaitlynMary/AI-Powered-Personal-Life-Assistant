import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConfirmationDialog } from './confirmation-dialog';
import { ConfirmationDialogService } from './confirmation-dialog.service';

describe('ConfirmationDialog', () => {
  let fixture: ComponentFixture<ConfirmationDialog>;
  let service: ConfirmationDialogService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    fixture = TestBed.createComponent(ConfirmationDialog);
    service = TestBed.inject(ConfirmationDialogService);
  });

  it('renders the supplied confirmation content and focuses Cancel', async () => {
    const result = service.confirm({
      title: 'Delete this task?',
      message: 'Are you sure you want to delete this task? This action cannot be undone.',
      confirmLabel: 'Yes, Delete',
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[role="alertdialog"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('#confirmation-title').textContent).toBe(
      'Delete this task?',
    );
    expect(fixture.nativeElement.querySelector('#confirmation-message').textContent).toBe(
      'Are you sure you want to delete this task? This action cannot be undone.',
    );
    expect(document.activeElement?.textContent?.trim()).toBe('Cancel');
    (fixture.nativeElement.querySelector('.confirmation-cancel') as HTMLButtonElement).click();
    expect(await result).toBe(false);
  });

  it('traps keyboard focus, cancels on Escape, and restores focus', async () => {
    const trigger = document.createElement('button');
    trigger.textContent = 'Delete task';
    document.body.append(trigger);
    trigger.focus();
    const result = service.confirm({
      title: 'Delete this task?',
      message: 'This action cannot be undone.',
      confirmLabel: 'Yes, Delete',
    });
    fixture.detectChanges();

    const cancel = fixture.nativeElement.querySelector('.confirmation-cancel') as HTMLButtonElement;
    const confirm = fixture.nativeElement.querySelector(
      '.confirmation-confirm',
    ) as HTMLButtonElement;
    const tabBack = new KeyboardEvent('keydown', {
      key: 'Tab',
      shiftKey: true,
      bubbles: true,
      cancelable: true,
    });
    cancel.dispatchEvent(tabBack);
    expect(tabBack.defaultPrevented).toBe(true);
    expect(document.activeElement).toBe(confirm);

    fixture.nativeElement.querySelector('[role="alertdialog"]').dispatchEvent(
      new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }),
    );
    expect(await result).toBe(false);
    fixture.detectChanges();
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(document.activeElement).toBe(trigger);
    trigger.remove();
  });

  it('resolves true when the confirm action is activated', async () => {
    const result = service.confirm({
      title: 'Delete this task?',
      message: 'This action cannot be undone.',
      confirmLabel: 'Yes, Delete',
    });
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('.confirmation-confirm') as HTMLButtonElement).click();

    expect(await result).toBe(true);
  });

  it('keeps the dialog open until the confirmed action succeeds', async () => {
    let finishAction!: () => void;
    const action = vi.fn(
      () =>
        new Promise<void>((resolve) => {
          finishAction = resolve;
        }),
    );
    const result = service.confirm(
      {
        title: 'Cancel this task?',
        message: 'This will move it out of active work.',
        confirmLabel: 'Cancel Task',
      },
      action,
    );
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('.confirmation-confirm') as HTMLButtonElement).click();
    expect(action).toHaveBeenCalledOnce();
    expect(service.actionPending()).toBe(true);
    expect(service.options()).not.toBeNull();

    finishAction();
    expect(await result).toBe(true);
    expect(service.options()).toBeNull();
  });

  it('keeps the dialog open and displays an error when the confirmed action fails', async () => {
    let failAction!: (error: Error) => void;
    const action = vi.fn(
      () =>
        new Promise<void>((_resolve, reject) => {
          failAction = reject;
        }),
    );
    const result = service.confirm(
      {
        title: 'Mark this task as missed?',
        message: 'This will preserve the task in your history.',
        confirmLabel: 'Mark as Missed',
      },
      action,
    );
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('.confirmation-confirm') as HTMLButtonElement).click();
    failAction(new Error('Task service unavailable'));
    await Promise.resolve();
    await Promise.resolve();
    fixture.detectChanges();

    expect(service.options()).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Task service unavailable');
    expect(service.actionPending()).toBe(false);

    (fixture.nativeElement.querySelector('.confirmation-cancel') as HTMLButtonElement).click();
    expect(await result).toBe(false);
  });
});

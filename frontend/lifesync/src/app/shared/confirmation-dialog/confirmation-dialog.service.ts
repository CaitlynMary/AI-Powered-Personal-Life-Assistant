import { DOCUMENT } from '@angular/common';
import { inject, Injectable, signal } from '@angular/core';

export interface ConfirmationOptions {
  title: string;
  message: string;
  confirmLabel: string;
}

@Injectable({ providedIn: 'root' })
export class ConfirmationDialogService {
  private readonly document = inject(DOCUMENT);
  private resolveConfirmation: ((confirmed: boolean) => void) | null = null;
  private confirmAction: (() => Promise<void>) | null = null;
  private previousFocus: HTMLElement | null = null;

  readonly options = signal<ConfirmationOptions | null>(null);
  readonly actionPending = signal(false);
  readonly actionError = signal<string | null>(null);

  confirm(options: ConfirmationOptions, action?: () => Promise<void>): Promise<boolean> {
    if (this.resolveConfirmation) {
      throw new Error('A confirmation dialog is already open.');
    }

    this.previousFocus =
      this.document.activeElement instanceof HTMLElement ? this.document.activeElement : null;
    this.confirmAction = action ?? null;
    this.actionPending.set(false);
    this.actionError.set(null);
    this.options.set(options);
    return new Promise<boolean>((resolve) => {
      this.resolveConfirmation = resolve;
    });
  }

  answer(confirmed: boolean): void {
    if (this.actionPending()) return;

    const resolve = this.resolveConfirmation;
    if (!resolve) return;

    if (confirmed && this.confirmAction) {
      void this.runConfirmAction();
      return;
    }

    this.close(confirmed);
  }

  private async runConfirmAction(): Promise<void> {
    const action = this.confirmAction;
    if (!action || this.actionPending()) return;

    this.actionPending.set(true);
    this.actionError.set(null);
    try {
      await action();
      this.close(true);
    } catch (error: unknown) {
      this.actionError.set(
        error instanceof Error ? error.message : 'Could not complete this action. Please try again.',
      );
      this.actionPending.set(false);
    }
  }

  private close(confirmed: boolean): void {
    const resolve = this.resolveConfirmation;
    if (!resolve) return;

    this.resolveConfirmation = null;
    this.confirmAction = null;
    this.options.set(null);
    this.actionPending.set(false);
    this.actionError.set(null);
    resolve(confirmed);

    const previousFocus = this.previousFocus;
    this.previousFocus = null;
    setTimeout(() => {
      if (previousFocus?.isConnected) previousFocus.focus();
    });
  }
}

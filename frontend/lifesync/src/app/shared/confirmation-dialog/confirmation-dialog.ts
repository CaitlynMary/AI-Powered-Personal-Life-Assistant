import { Component, ElementRef, inject, ViewChild } from '@angular/core';
import { ConfirmationDialogService } from './confirmation-dialog.service';

@Component({
  selector: 'app-confirmation-dialog',
  standalone: true,
  templateUrl: './confirmation-dialog.html',
  styleUrl: './confirmation-dialog.css',
})
export class ConfirmationDialog {
  readonly confirmation = inject(ConfirmationDialogService);

  @ViewChild('dialog') private dialog?: ElementRef<HTMLElement>;

  @ViewChild('cancelButton') set cancelButton(button: ElementRef<HTMLButtonElement> | undefined) {
    button?.nativeElement.focus();
  }

  dismissOnBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) this.confirmation.answer(false);
  }

  handleKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      event.preventDefault();
      this.confirmation.answer(false);
      return;
    }

    if (event.key !== 'Tab') return;
    const buttons = this.dialog?.nativeElement.querySelectorAll<HTMLButtonElement>(
      'button:not([disabled])',
    );
    if (!buttons?.length) return;

    const first = buttons[0];
    const last = buttons[buttons.length - 1];
    if (event.shiftKey && this.dialog?.nativeElement.ownerDocument.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && this.dialog?.nativeElement.ownerDocument.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  }
}

import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class LayoutService {
  /** Desktop sidebar collapsed to icon rail. */
  readonly sidebarCollapsed = signal(false);

  /** Mobile drawer open state. */
  readonly mobileNavOpen = signal(false);

  toggleSidebar(): void {
    this.sidebarCollapsed.update((value) => !value);
  }

  openMobileNav(): void {
    this.mobileNavOpen.set(true);
  }

  closeMobileNav(): void {
    this.mobileNavOpen.set(false);
  }

  toggleMobileNav(): void {
    this.mobileNavOpen.update((value) => !value);
  }
}

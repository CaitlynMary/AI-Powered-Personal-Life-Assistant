import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { LucideDynamicIcon } from '@lucide/angular';
import { map } from 'rxjs';

/** Lightweight placeholder for modules not yet built in Stage 1. */
@Component({
  selector: 'app-coming-soon-page',
  standalone: true,
  imports: [LucideDynamicIcon],
  template: `
    <section class="ls-card mx-auto max-w-xl bg-ls-surface p-8 text-center">
      <div
        class="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-soft-pink text-ls-text"
      >
        <svg lucideIcon="sparkles" [size]="26"></svg>
      </div>
      <h1 class="text-2xl font-extrabold text-ls-text">{{ title() }}</h1>
      <p class="mt-2 text-sm font-medium text-ls-text-muted">
        This module is intentionally scaffolded for navigation. We will design and build it next
        after you approve the dashboard stage.
      </p>
    </section>
  `,
})
export class ComingSoonPage {
  private readonly route = inject(ActivatedRoute);

  readonly title = toSignal(
    this.route.title.pipe(map((value) => value?.split('·')[0]?.trim() || 'Coming soon')),
    { initialValue: 'Coming soon' },
  );
}

import { Component, input } from '@angular/core';
import { QuoteLeaf } from '../../../../shared/illustrations/quote-leaf';

@Component({
  selector: 'app-motivational-quote',
  standalone: true,
  imports: [QuoteLeaf],
  template: `
    <section
      class="ls-card relative flex h-full flex-col justify-between overflow-hidden bg-[var(--ls-card-quote)] p-5"
    >
      <div
        class="pointer-events-none absolute -bottom-6 -right-4 h-28 w-28 rounded-full bg-mint/25 blur-xl"
      ></div>
      <div class="relative z-10 flex items-start justify-between gap-3">
        <p class="text-xs font-bold uppercase tracking-[0.16em] text-purple">Daily inspiration</p>
        <app-quote-leaf />
      </div>
      <blockquote class="relative z-10 mt-4">
        <p class="break-words font-quote text-3xl leading-snug text-ls-text dark:text-cream">
          “{{ quote() }}”
        </p>
        <footer class="mt-3 text-sm font-semibold text-ls-text/55 dark:text-cream/65">
          — {{ author() }}
        </footer>
      </blockquote>
    </section>
  `,
})
export class MotivationalQuote {
  readonly quote = input.required<string>();
  readonly author = input.required<string>();
}

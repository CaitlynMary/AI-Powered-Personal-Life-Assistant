import { Component, input } from '@angular/core';
import { WelcomeIllustration } from '../../../../shared/illustrations/welcome-illustration';

@Component({
  selector: 'app-welcome-banner',
  standalone: true,
  imports: [WelcomeIllustration],
  template: `
    <section
      class="ls-card ls-title-card relative overflow-hidden bg-ls-surface p-5 sm:p-7"
    >
      <div class="relative z-10 grid items-center gap-6 lg:grid-cols-[1.2fr_0.8fr]">
        <div>
          <p class="ls-title-eyebrow mb-2 text-sm font-semibold">{{ dateLabel() }}</p>
          <h1 class="text-3xl font-extrabold tracking-tight text-plum dark:text-cream sm:text-4xl">
            {{ greeting() }}
          </h1>
          <p class="mt-3 max-w-xl text-base font-medium text-ls-text/70">
            {{ subtitle() }}
          </p>
        </div>
        <div class="mx-auto h-36 w-full max-w-sm lg:h-40">
          <app-welcome-illustration />
        </div>
      </div>
    </section>
  `,
})
export class WelcomeBanner {
  readonly greeting = input.required<string>();
  readonly dateLabel = input.required<string>();
  readonly subtitle = input.required<string>();
}

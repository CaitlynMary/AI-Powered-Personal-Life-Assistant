import { Component } from '@angular/core';
import { LifesyncLogo } from '../../shared/illustrations/lifesync-logo';
import { WelcomeIllustration } from '../../shared/illustrations/welcome-illustration';

@Component({
  selector: 'app-auth-shell',
  standalone: true,
  imports: [LifesyncLogo, WelcomeIllustration],
  template: `
    <main class="min-h-dvh bg-ls-bg px-4 py-8 text-ls-text sm:px-6">
      <div class="mx-auto grid min-h-[calc(100dvh-4rem)] max-w-6xl items-center gap-8 lg:grid-cols-[1.05fr_0.95fr]">
        <section class="hidden lg:block">
          <div class="mb-6 flex items-center gap-3">
            <app-lifesync-logo [size]="52" />
            <div>
              <p class="text-2xl font-extrabold tracking-tight">LifeSync</p>
              <p class="text-sm font-semibold text-orchid">Personal Life Assistant</p>
            </div>
          </div>
          <h1 class="max-w-md text-4xl font-extrabold leading-tight">
            Stay organised, focused, and create a better you.
          </h1>
          <p class="mt-4 max-w-md text-base font-medium text-plum/75">
            Tasks, goals, habits, and your AI assistant — all in one colourful place.
          </p>
          <div class="mt-8 max-w-md overflow-hidden rounded-3xl shadow-card">
            <app-welcome-illustration />
          </div>
        </section>

        <section class="ls-card mx-auto w-full max-w-md bg-ls-surface p-6 sm:p-8">
          <div class="mb-6 flex items-center gap-3 lg:hidden">
            <app-lifesync-logo [size]="42" />
            <div>
              <p class="text-xl font-extrabold">LifeSync</p>
              <p class="text-xs font-semibold text-orchid">Personal Life Assistant</p>
            </div>
          </div>
          <ng-content />
        </section>
      </div>
    </main>
  `,
})
export class AuthShell {}

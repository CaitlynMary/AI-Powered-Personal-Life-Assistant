import { Component } from '@angular/core';

@Component({
  selector: 'app-welcome-illustration',
  standalone: true,
  template: `
    <svg
      viewBox="0 0 280 160"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      class="h-full w-full"
      aria-hidden="true"
    >
      <defs>
        <linearGradient id="sky" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="var(--bg-secondary)" />
          <stop offset="55%" stop-color="var(--powder-blue)" />
          <stop offset="100%" stop-color="var(--aqua)" />
        </linearGradient>
        <linearGradient id="hillA" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stop-color="var(--accent-purple)" />
          <stop offset="100%" stop-color="var(--bg-sidebar)" />
        </linearGradient>
        <linearGradient id="hillB" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stop-color="var(--aqua)" />
          <stop offset="100%" stop-color="var(--powder-blue)" />
        </linearGradient>
      </defs>
      <rect width="280" height="160" rx="28" fill="url(#sky)" />
      <circle cx="198" cy="48" r="28" fill="var(--accent-primary)" opacity="0.8" />
      <circle cx="198" cy="48" r="18" fill="var(--warning)" opacity="0.35" />
      <path
        d="M0 118c28-28 62-42 98-38 30 3 52 18 84 16 24-2 48-14 70-8v52H0v-22Z"
        fill="url(#hillA)"
      />
      <path
        d="M0 132c36-20 72-24 108-14 34 10 58 8 92-6 24-10 48-8 80 4v44H0v-28Z"
        fill="url(#hillB)"
        opacity="0.85"
      />
      <path d="M42 118c4-16 12-28 18-34 2 10 6 20 4 34h-22Z" fill="var(--warning)" />
      <path d="M58 118c3-12 8-20 12-24 1 8 3 16 1 24H58Z" fill="var(--aqua)" />
      <path d="M230 120c3-14 10-24 15-28 2 9 4 18 2 28h-17Z" fill="var(--aqua)" />
    </svg>
  `,
})
export class WelcomeIllustration {}

import { Component } from '@angular/core';

@Component({
  selector: 'app-ai-assistant-illustration',
  standalone: true,
  template: `
    <svg
      viewBox="0 0 160 140"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      class="h-full w-full"
      aria-hidden="true"
    >
      <ellipse cx="80" cy="126" rx="46" ry="8" fill="var(--accent-purple)" opacity="0.18" />
      <rect x="48" y="42" width="64" height="58" rx="18" fill="var(--robot-body)" />
      <rect x="56" y="52" width="48" height="28" rx="10" fill="var(--robot-screen)" />
      <circle cx="70" cy="66" r="5" fill="var(--aqua)" />
      <circle cx="90" cy="66" r="5" fill="var(--aqua)" />
      <path d="M68 78c4 4 12 4 16 0" stroke="var(--accent-magenta)" stroke-width="3" stroke-linecap="round" />
      <rect x="70" y="28" width="20" height="16" rx="6" fill="var(--aqua)" />
      <circle cx="80" cy="24" r="6" fill="var(--warning)" />
      <path d="M40 70h10M110 70h10" stroke="var(--accent-purple)" stroke-width="4" stroke-linecap="round" />
      <circle cx="34" cy="70" r="7" fill="var(--accent-magenta)" />
      <circle cx="126" cy="70" r="7" fill="var(--accent-magenta)" />
      <path d="M62 100h36c4 0 8 4 8 8v6H54v-6c0-4 4-8 8-8Z" fill="var(--robot-body)" />
      <path d="M20 30c8-10 22-8 28 2" stroke="var(--warning)" stroke-width="3" stroke-linecap="round" />
      <path d="M118 24c8-8 20-6 26 4" stroke="var(--aqua)" stroke-width="3" stroke-linecap="round" />
      <circle cx="24" cy="22" r="4" fill="var(--aqua)" />
      <circle cx="140" cy="20" r="4" fill="var(--warning)" />
    </svg>
  `,
})
export class AiAssistantIllustration {}

import { Component } from '@angular/core';

@Component({
  selector: 'app-sidebar-deco',
  standalone: true,
  template: `
    <svg
      viewBox="0 0 240 120"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      class="h-full w-full"
      aria-hidden="true"
    >
      <path
        d="M0 70c40-30 80-20 120-8 40 12 80 20 120 4v54H0V70Z"
        fill="var(--brand-lilac)"
        opacity="0.55"
      />
      <path
        d="M0 88c36-18 78-10 118 2 42 12 78 10 122-6v36H0V88Z"
        fill="var(--accent-purple)"
        opacity="0.85"
      />
      <path d="M168 78c6-22 18-38 28-46 4 14 8 30 4 46h-32Z" fill="var(--accent-primary)" />
      <path d="M188 78c4-16 12-28 18-34 2 12 4 24 1 34h-19Z" fill="var(--brand-teal)" />
      <circle cx="204" cy="40" r="5" fill="var(--brand-soft-pink)" />
    </svg>
  `,
})
export class SidebarDeco {}

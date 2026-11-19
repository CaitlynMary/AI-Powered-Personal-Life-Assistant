import { Component } from '@angular/core';

@Component({
  selector: 'app-quote-leaf',
  standalone: true,
  template: `
    <svg
      viewBox="0 0 64 64"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      class="h-14 w-14"
      aria-hidden="true"
    >
      <path
        d="M32 8c-4 12-14 20-22 24 8 3 15 10 19 20 3-10 10-17 19-20-8-3-14-10-16-24Z"
        fill="var(--aqua)"
      />
      <path
        d="M34 14c-2 9-9 15-15 18 6 2 11 8 14 15 2-8 8-13 15-15-6-3-11-8-14-18Z"
        fill="var(--rose)"
      />
      <path d="M32 30v22" stroke="var(--accent-purple)" stroke-width="2" stroke-linecap="round" />
    </svg>
  `,
})
export class QuoteLeaf {}

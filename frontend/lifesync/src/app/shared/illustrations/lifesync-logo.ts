import { Component, input } from '@angular/core';

@Component({
  selector: 'app-lifesync-logo',
  standalone: true,
  template: `
    <svg
      [attr.width]="size()"
      [attr.height]="size()"
      viewBox="0 0 48 48"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <path
        d="M24 6c-2.5 7-9 11.5-14 14 5 1.8 9.2 5.8 11.5 11.5C23.2 25.8 27.8 21.5 34 19c-5.5-2.2-8.8-6.8-10-13Z"
        fill="var(--brand-teal)"
      />
      <path
        d="M28 10c-1.2 5.5-5.5 9.2-10 11.5 4.2 1.2 7.8 4.5 9.5 9 1.2-5.2 4.8-8.8 10-10.5-4.5-1.8-7.8-5.2-9.5-10Z"
        fill="var(--accent-primary)"
      />
      <path
        d="M20 14c-1 4.8-4.8 8-8.8 10 3.8 1.2 6.8 4 8.2 8 1-4.5 4-7.8 8.6-9.5-4-1.8-6.8-4.8-8-8.5Z"
        fill="var(--brand-lilac)"
      />
      <path d="M24 28v12" stroke="var(--text-on-dark)" stroke-width="2.5" stroke-linecap="round" />
    </svg>
  `,
})
export class LifesyncLogo {
  readonly size = input(40);
}

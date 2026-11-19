import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { LandingPage } from './landing.page';

describe('LandingPage', () => {
  let fixture: ComponentFixture<LandingPage>;

  beforeEach(async () => {
    localStorage.removeItem('lifesync-theme');
    document.documentElement.classList.remove('dark');
    document.documentElement.removeAttribute('data-theme');
    await TestBed.configureTestingModule({
      imports: [LandingPage],
      providers: [provideRouter([])],
    }).compileComponents();
    fixture = TestBed.createComponent(LandingPage);
    fixture.detectChanges();
  });

  it('renders the requested hero copy and uses the supplied illustration asset', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Plan Focus Grow');
    expect(text).toContain('Your AI-Powered Personal Life Assistant');
    expect(text).toContain(
      'Manage your tasks, goals, habits and time in one place. Stay organised, build better routines and create a more balanced, productive life.',
    );
    expect(
      (fixture.nativeElement as HTMLElement).querySelector(
        '.landing-leaf-decoration app-quote-leaf',
      ),
    ).not.toBeNull();
    const image = (fixture.nativeElement as HTMLElement).querySelector<HTMLImageElement>(
      '.hero-illustration',
    );
    expect(image).not.toBeNull();
    expect(image?.getAttribute('src')).toBe('/images/lifesync-landing-illustration.png');
    expect(image?.getAttribute('width')).toBe('980');
    expect(image?.getAttribute('height')).toBe('768');
    expect(image?.getAttribute('alt')).toContain('notebook with a checklist');
    const floatingFeatures = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLElement>(
      '.floating-feature',
    );
    expect(floatingFeatures).toHaveLength(5);
    expect(Array.from(floatingFeatures, (feature) => feature.textContent?.trim())).toEqual([
      'Tasks',
      'Goals',
      'Habits',
      'Calendar',
      'AI Assistant',
    ]);
  });

  it('links account actions to the existing auth routes and sections to in-page anchors', () => {
    const links = Array.from(fixture.nativeElement.querySelectorAll('a')) as HTMLAnchorElement[];
    expect(
      links.some(
        (link) =>
          link.textContent?.includes('Get Started') && link.getAttribute('href') === '/register',
      ),
    ).toBe(true);
    expect(
      links.some(
        (link) =>
          link.textContent?.includes('Sign Up') && link.getAttribute('href') === '/register',
      ),
    ).toBe(true);
    expect(
      links.some(
        (link) => link.textContent?.includes('Log In') && link.getAttribute('href') === '/login',
      ),
    ).toBe(true);
    expect(
      links.some(
        (link) =>
          link.textContent?.includes('See How It Works') &&
          link.getAttribute('href') === '#how-it-works',
      ),
    ).toBe(true);
    expect(
      links.some(
        (link) =>
          link.textContent?.includes('Features') && link.getAttribute('href') === '#features',
      ),
    ).toBe(true);
    expect(
      links.some(
        (link) => link.textContent?.includes('About') && link.getAttribute('href') === '#about',
      ),
    ).toBe(true);
    expect(
      Array.from(
        (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLAnchorElement>(
          '.desktop-nav .nav-link',
        ),
        (link) => link.textContent?.trim(),
      ),
    ).toEqual(['Home', 'Features', 'About']);
    expect(fixture.nativeElement.querySelector('#contact')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.landing-copyright')?.textContent?.trim()).toBe(
      '© 2026 LifeSync. All rights reserved.',
    );
    expect(
      (fixture.nativeElement as HTMLElement)
        .querySelector('.landing-theme-toggle')
        ?.getAttribute('aria-label'),
    ).toBe('Switch to dark theme');
  });

  it('renders the eight feature cards in the specified order with their existing copy', () => {
    const cards = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLElement>('.feature-card'),
    );

    expect(cards).toHaveLength(8);
    expect(cards.map((card) => card.querySelector('h3')?.textContent?.trim())).toEqual([
      'Task Management',
      'Goal Tracking',
      'Habit Tracking',
      'Calendar',
      'Daily Planner',
      'Notes and Journal',
      'Reminders and Notifications',
      'AI Personal Assistant',
    ]);
    expect(cards.map((card) => card.querySelector('p')?.textContent?.trim())).toEqual([
      'Organise priorities, due dates, and the next small step.',
      'Turn meaningful goals into milestones and visible progress.',
      'Build steady routines and see your streaks take root.',
      'Bring events and important dates into one clear view.',
      'Shape a balanced day around the tasks that matter.',
      'Keep reflections and useful thoughts close at hand.',
      'Get timely nudges and stay up to date on your plans.',
      'Ask for ideas and get help planning your next steps.',
    ]);
  });

  it('toggles the shared theme service and persists the selection', () => {
    const themeToggle = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
      '.landing-theme-toggle',
    );
    expect(themeToggle).not.toBeNull();

    themeToggle?.click();
    fixture.detectChanges();

    expect(document.documentElement.classList.contains('dark')).toBe(true);
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    expect(localStorage.getItem('lifesync-theme')).toBe('dark');
    expect(themeToggle?.getAttribute('aria-label')).toBe('Switch to light theme');
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('.feature-card')).toHaveLength(
      8,
    );
    expect(
      (fixture.nativeElement as HTMLElement)
        .querySelector<HTMLImageElement>('.hero-illustration')
        ?.getAttribute('src'),
    ).toBe('/images/lifesync-landing-illustration-dark.png');

    themeToggle?.click();
    fixture.detectChanges();

    expect(document.documentElement.classList.contains('dark')).toBe(false);
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    expect(localStorage.getItem('lifesync-theme')).toBe('light');
    expect(
      (fixture.nativeElement as HTMLElement)
        .querySelector<HTMLImageElement>('.hero-illustration')
        ?.getAttribute('src'),
    ).toBe('/images/lifesync-landing-illustration.png');
  });

  it('restores a persisted dark theme when the landing page is initialized', async () => {
    TestBed.resetTestingModule();
    localStorage.setItem('lifesync-theme', 'dark');
    await TestBed.configureTestingModule({
      imports: [LandingPage],
      providers: [provideRouter([])],
    }).compileComponents();
    const savedFixture = TestBed.createComponent(LandingPage);
    savedFixture.detectChanges();

    expect(document.documentElement.classList.contains('dark')).toBe(true);
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    expect(
      (savedFixture.nativeElement as HTMLElement)
        .querySelector('.landing-theme-toggle')
        ?.getAttribute('aria-label'),
    ).toBe('Switch to light theme');
    expect(
      (savedFixture.nativeElement as HTMLElement)
        .querySelector<HTMLImageElement>('.hero-illustration')
        ?.getAttribute('src'),
    ).toBe('/images/lifesync-landing-illustration-dark.png');
  });
});

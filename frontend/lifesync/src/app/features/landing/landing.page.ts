import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LifesyncLogo } from '../../shared/illustrations/lifesync-logo';
import { QuoteLeaf } from '../../shared/illustrations/quote-leaf';
import { ThemeService } from '../../core/services/theme.service';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [RouterLink, LifesyncLogo, QuoteLeaf],
  templateUrl: './landing.page.html',
})
export class LandingPage {
  private readonly theme = inject(ThemeService);
  readonly mode = this.theme.mode;

  readonly features = [
    {
      icon: 'check-square',
      title: 'Task Management',
      description: 'Organise priorities, due dates, and the next small step.',
      tone: 'lavender',
    },
    {
      icon: 'target',
      title: 'Goal Tracking',
      description: 'Turn meaningful goals into milestones and visible progress.',
      tone: 'rose',
    },
    {
      icon: 'sprout',
      title: 'Habit Tracking',
      description: 'Build steady routines and see your streaks take root.',
      tone: 'mint',
    },
    {
      icon: 'calendar-days',
      title: 'Calendar',
      description: 'Bring events and important dates into one clear view.',
      tone: 'blue',
    },
    {
      icon: 'list-checks',
      title: 'Daily Planner',
      description: 'Shape a balanced day around the tasks that matter.',
      tone: 'rose',
    },
    {
      icon: 'notebook-pen',
      title: 'Notes and Journal',
      description: 'Keep reflections and useful thoughts close at hand.',
      tone: 'lavender',
    },
    {
      icon: 'bell',
      title: 'Reminders and Notifications',
      description: 'Get timely nudges and stay up to date on your plans.',
      tone: 'blue',
    },
    {
      icon: 'bot',
      title: 'AI Personal Assistant',
      description: 'Ask for ideas and get help planning your next steps.',
      tone: 'mint',
    },
  ];

  toggleTheme(): void {
    this.theme.toggle();
  }
}

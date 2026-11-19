import { NgClass } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { LucideDynamicIcon } from '@lucide/angular';
import { AuthService } from '../../auth/auth.service';
import { LayoutService } from '../../services/layout.service';
import { LifesyncLogo } from '../../../shared/illustrations/lifesync-logo';
import { SidebarDeco } from '../../../shared/illustrations/sidebar-deco';

interface NavItem {
  label: string;
  path: string;
  icon: string;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [NgClass, RouterLink, RouterLinkActive, LucideDynamicIcon, LifesyncLogo, SidebarDeco],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css',
})
export class Sidebar {
  private readonly layout = inject(LayoutService);
  private readonly auth = inject(AuthService);

  readonly collapsed = this.layout.sidebarCollapsed;
  readonly mobileOpen = this.layout.mobileNavOpen;

  readonly primaryNav: NavItem[] = [
    { label: 'Dashboard', path: '/dashboard', icon: 'layout-dashboard' },
    { label: 'Tasks', path: '/tasks', icon: 'check-square' },
    { label: 'Goals', path: '/goals', icon: 'target' },
    { label: 'Habits', path: '/habits', icon: 'repeat' },
    { label: 'Calendar', path: '/calendar', icon: 'calendar-days' },
    { label: 'Planner', path: '/planner', icon: 'list-checks' },
    { label: 'Notes', path: '/notes', icon: 'notebook-pen' },
    { label: 'Journal', path: '/journal', icon: 'notebook-pen' },
    { label: 'AI Assistant', path: '/ai-assistant', icon: 'bot' },
    { label: 'Reminders', path: '/reminders', icon: 'bell' },
    { label: 'Notifications', path: '/notifications', icon: 'inbox' },
  ];

  readonly secondaryNav: NavItem[] = [{ label: 'Settings', path: '/settings', icon: 'settings' }];

  closeMobile(): void {
    this.layout.closeMobileNav();
  }

  toggleCollapse(): void {
    this.layout.toggleSidebar();
  }

  logout(): void {
    this.closeMobile();
    this.auth.logout().subscribe();
  }
}

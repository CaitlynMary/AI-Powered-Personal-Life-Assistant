import { Component, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideDynamicIcon } from '@lucide/angular';
import { AuthService } from '../../auth/auth.service';
import { LayoutService } from '../../services/layout.service';
import { ThemeService } from '../../services/theme.service';
import { NotificationMenu } from '../../../features/notifications/notification-menu/notification-menu';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [FormsModule, LucideDynamicIcon, NotificationMenu],
  templateUrl: './header.html',
})
export class Header {
  private readonly layout = inject(LayoutService);
  private readonly theme = inject(ThemeService);
  private readonly auth = inject(AuthService);

  readonly profile = this.auth.currentUserProfile;
  readonly profileLoading = this.auth.profileLoading;
  readonly profileError = this.auth.profileError;
  readonly avatarInitials = computed(() => {
    const fullName = this.profile()?.fullName.trim();
    return fullName
      ? fullName
          .split(/\s+/)
          .slice(0, 2)
          .map((part) => part.charAt(0))
          .join('')
          .toUpperCase()
      : 'LS';
  });

  searchQuery = '';

  readonly mode = this.theme.mode;

  openMobileNav(): void {
    this.layout.openMobileNav();
  }

  toggleTheme(): void {
    this.theme.toggle();
  }
}

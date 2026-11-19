import { NgClass } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideDynamicIcon } from '@lucide/angular';
import { DashboardTask, TaskPriority } from '../../../../shared/models/dashboard.models';

@Component({
  selector: 'app-todays-tasks',
  standalone: true,
  imports: [NgClass, RouterLink, LucideDynamicIcon],
  templateUrl: './todays-tasks.html',
})
export class TodaysTasks {
  readonly tasks = input.required<DashboardTask[]>();
  readonly loading = input(false);
  readonly hasError = input(false);
  readonly updatingTaskId = input<string | null>(null);
  readonly toggleTask = output<string>();
  readonly addTask = output<void>();

  priorityClass(priority: TaskPriority): string {
    switch (priority) {
      case 'high':
        return 'bg-tangerine/15 text-plum';
      case 'medium':
        return 'bg-rose text-plum';
      case 'urgent':
        return 'bg-burgundy text-white';
      default:
        return 'bg-mint/40 text-ls-text';
    }
  }

  statusLabel(status: DashboardTask['status']): string {
    switch (status) {
      case 'IN_PROGRESS':
        return 'In progress';
      case 'COMPLETED':
        return 'Completed';
      case 'OVERDUE':
        return 'Overdue';
      case 'MISSED':
        return 'Missed';
      case 'CANCELLED':
        return 'Cancelled';
      default:
        return 'To do';
    }
  }
}

ALTER TABLE IF EXISTS notifications
    DROP CONSTRAINT IF EXISTS notifications_notification_type_check;

ALTER TABLE IF EXISTS notifications
    ADD CONSTRAINT notifications_notification_type_check
    CHECK (notification_type IN (
        'REMINDER',
        'TASK_DEADLINE_APPROACHING',
        'TASK_OVERDUE',
        'TASK_COMPLETED',
        'TASK_MISSED',
        'TASK_RESCHEDULED',
        'MILESTONE_COMPLETED',
        'HABIT_COMPLETED',
        'AI_DAILY_PLAN_READY',
        'SYSTEM'
    ));

ALTER TABLE IF EXISTS tasks
    DROP CONSTRAINT IF EXISTS tasks_status_check;

ALTER TABLE IF EXISTS tasks
    ADD CONSTRAINT tasks_status_check
    CHECK (status IN ('TODO', 'IN_PROGRESS', 'COMPLETED', 'MISSED', 'CANCELLED'));

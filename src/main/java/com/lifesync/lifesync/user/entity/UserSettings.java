package com.lifesync.lifesync.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
public class UserSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "task_reminders_enabled", nullable = false)
    private boolean taskRemindersEnabled = true;

    @Column(name = "goal_milestone_updates_enabled", nullable = false)
    private boolean goalMilestoneUpdatesEnabled = true;

    @Column(name = "habit_reminders_enabled", nullable = false)
    private boolean habitRemindersEnabled = true;

    @Column(name = "ai_planner_updates_enabled", nullable = false)
    private boolean aiPlannerUpdatesEnabled = true;

    @Column(name = "time_zone_id", nullable = false, length = 100)
    private String timeZoneId = "DEVICE";

    @Column(name = "date_format", nullable = false, length = 20)
    private String dateFormat = "LOCALE";

    @Column(name = "time_format", nullable = false, length = 10)
    private String timeFormat = "LOCALE";

    @Column(name = "week_start", nullable = false, length = 10)
    private String weekStart = "MONDAY";

    @Column(name = "ai_response_style", nullable = false, length = 20)
    private String aiResponseStyle = "BALANCED";

    @Column(name = "ai_personalization_enabled", nullable = false)
    private boolean aiPersonalizationEnabled = true;
}

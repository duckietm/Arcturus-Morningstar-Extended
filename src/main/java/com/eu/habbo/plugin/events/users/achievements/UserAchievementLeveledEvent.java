package com.eu.habbo.plugin.events.users.achievements;

import com.eu.habbo.habbohotel.achievements.Achievement;
import com.eu.habbo.habbohotel.achievements.AchievementLevel;
import com.eu.habbo.habbohotel.users.Habbo;

public class UserAchievementLeveledEvent extends UserAchievementEvent {

    public final AchievementLevel oldLevel;


    public final AchievementLevel newLevel;


    public UserAchievementLeveledEvent(Habbo habbo, Achievement achievement, AchievementLevel oldLevel, AchievementLevel newLevel) {
        super(habbo, achievement);

        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }
}

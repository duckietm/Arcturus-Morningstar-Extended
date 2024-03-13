package com.eu.habbo.plugin.events.users.achievements;

import com.eu.habbo.habbohotel.achievements.Achievement;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.plugin.events.users.UserEvent;

public abstract class UserAchievementEvent extends UserEvent {

    public final Achievement achievement;


    public UserAchievementEvent(Habbo habbo, Achievement achievement) {
        super(habbo);

        this.achievement = achievement;
    }
}

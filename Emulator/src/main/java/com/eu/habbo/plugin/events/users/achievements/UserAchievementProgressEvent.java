package com.eu.habbo.plugin.events.users.achievements;

import com.eu.habbo.habbohotel.achievements.Achievement;
import com.eu.habbo.habbohotel.users.Habbo;

public class UserAchievementProgressEvent extends UserAchievementEvent {

    public final int progressed;


    public UserAchievementProgressEvent(Habbo habbo, Achievement achievement, int progressed) {
        super(habbo, achievement);

        this.progressed = progressed;
    }
}

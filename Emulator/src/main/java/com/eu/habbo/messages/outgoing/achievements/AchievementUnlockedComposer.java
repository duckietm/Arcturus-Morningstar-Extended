package com.eu.habbo.messages.outgoing.achievements;

import com.eu.habbo.habbohotel.achievements.Achievement;
import com.eu.habbo.habbohotel.achievements.AchievementLevel;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class AchievementUnlockedComposer extends MessageComposer {
    private final Achievement achievement;
    private final Habbo habbo;

    public AchievementUnlockedComposer(Habbo habbo, Achievement achievement) {
        this.achievement = achievement;
        this.habbo = habbo;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.AchievementUnlockedComposer);

        AchievementLevel level = this.achievement.getLevelForProgress(this.habbo.getHabboStats().getAchievementProgress(this.achievement));
        this.response.appendInt(this.achievement.id);
        this.response.appendInt(level.level);
        this.response.appendInt(144);
        this.response.appendString("ACH_" + this.achievement.name + level.level);
        this.response.appendInt(level.rewardAmount);
        this.response.appendInt(level.rewardType);
        this.response.appendInt(0);
        this.response.appendInt(10);
        this.response.appendInt(21);
        this.response.appendString(level.level > 1 ? "ACH_" + this.achievement.name + (level.level - 1) : "");
        this.response.appendString(this.achievement.category.name());
        this.response.appendBoolean(true);
        return this.response;
    }
}

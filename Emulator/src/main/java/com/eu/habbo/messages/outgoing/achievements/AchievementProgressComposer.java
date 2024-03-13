package com.eu.habbo.messages.outgoing.achievements;

import com.eu.habbo.habbohotel.achievements.Achievement;
import com.eu.habbo.habbohotel.achievements.AchievementLevel;
import com.eu.habbo.habbohotel.achievements.AchievementManager;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class AchievementProgressComposer extends MessageComposer {
    private final Habbo habbo;
    private final Achievement achievement;

    public AchievementProgressComposer(Habbo habbo, Achievement achievement) {
        this.habbo = habbo;
        this.achievement = achievement;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.AchievementProgressComposer);

        int achievementProgress;
        AchievementLevel currentLevel;
        AchievementLevel nextLevel;

        achievementProgress = this.habbo.getHabboStats().getAchievementProgress(this.achievement);
        currentLevel = this.achievement.getLevelForProgress(achievementProgress);
        nextLevel = this.achievement.getNextLevel(currentLevel != null ? currentLevel.level : 0);

        if (currentLevel != null && currentLevel.level == this.achievement.levels.size())
            nextLevel = null;

        int targetLevel = 1;

        if (nextLevel != null)
            targetLevel = nextLevel.level;

        if (currentLevel != null && currentLevel.level == this.achievement.levels.size())
            targetLevel = currentLevel.level;

        this.response.appendInt(this.achievement.id); //ID
        this.response.appendInt(targetLevel); //Target level
        this.response.appendString("ACH_" + this.achievement.name + targetLevel); //Target badge code
        this.response.appendInt(currentLevel != null ? currentLevel.progress : 0); //Last level progress needed
        this.response.appendInt(nextLevel != null ? nextLevel.progress : 0); //Progress needed
        this.response.appendInt(nextLevel != null ? nextLevel.rewardAmount : 0); //Reward amount
        this.response.appendInt(nextLevel != null ? nextLevel.rewardType : 0); //Reward currency ID
        this.response.appendInt(achievementProgress == -1 ? 0 : achievementProgress); //Current progress
        this.response.appendBoolean(AchievementManager.hasAchieved(this.habbo, this.achievement)); //Achieved? (Current Progress == MaxLevel.Progress)
        this.response.appendString(this.achievement.category.toString().toLowerCase()); //Category
        this.response.appendString(""); //Empty, completly unused in client code
        this.response.appendInt(this.achievement.levels.size()); //Count of total levels in this achievement
        this.response.appendInt(0); //1 = Progressbar visible if the achievement is completed

        return this.response;
    }
}
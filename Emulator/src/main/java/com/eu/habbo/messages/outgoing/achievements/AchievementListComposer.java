package com.eu.habbo.messages.outgoing.achievements;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.achievements.Achievement;
import com.eu.habbo.habbohotel.achievements.AchievementLevel;
import com.eu.habbo.habbohotel.achievements.AchievementManager;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AchievementListComposer extends MessageComposer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AchievementListComposer.class);

    private final Habbo habbo;

    public AchievementListComposer(Habbo habbo) {
        this.habbo = habbo;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.AchievementListComposer);

        try {
            this.response.appendInt(Emulator.getGameEnvironment().getAchievementManager().getAchievements().size());

            for (Achievement achievement : Emulator.getGameEnvironment().getAchievementManager().getAchievements().values()) {
                int achievementProgress;
                AchievementLevel currentLevel;
                AchievementLevel nextLevel;

                achievementProgress = this.habbo.getHabboStats().getAchievementProgress(achievement);
                currentLevel = achievement.getLevelForProgress(achievementProgress);
                nextLevel = achievement.getNextLevel(currentLevel != null ? currentLevel.level : 0);

                this.response.appendInt(achievement.id); //ID
                this.response.appendInt(nextLevel != null ? nextLevel.level : currentLevel != null ? currentLevel.level : 0); //
                this.response.appendString("ACH_" + achievement.name + (nextLevel != null ? nextLevel.level : currentLevel != null ? currentLevel.level : 0)); //Target badge code
                this.response.appendInt(currentLevel != null ? currentLevel.progress : 0); //Last level progress needed
                this.response.appendInt(nextLevel != null ? nextLevel.progress : -1); //Progress needed
                this.response.appendInt(nextLevel != null ? nextLevel.rewardAmount : -1); //Reward amount
                this.response.appendInt(nextLevel != null ? nextLevel.rewardType : -1); //Reward currency ID
                this.response.appendInt(achievementProgress <= 0 ? 0 : achievementProgress); //Current progress
                this.response.appendBoolean(AchievementManager.hasAchieved(this.habbo, achievement)); //Achieved? (Current Progress == MaxLevel.Progress)
                this.response.appendString(achievement.category.toString().toLowerCase()); //Category
                this.response.appendString(""); //Empty, completly unused in client code
                this.response.appendInt(achievement.levels.size()); //Count of total levels in this achievement
                this.response.appendInt(AchievementManager.hasAchieved(this.habbo, achievement) ? 1 : 0); //1 = Progressbar visible if the achievement is completed
            }

            this.response.appendString("");
        } catch (Exception e) {
            LOGGER.error("Caught exception", e);
        }

        return this.response;
    }
}
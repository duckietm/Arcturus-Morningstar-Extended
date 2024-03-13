package com.eu.habbo.messages.incoming.modtool;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.achievements.AchievementManager;
import com.eu.habbo.messages.incoming.MessageHandler;

public class StartSafetyQuizEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        String quizName = this.packet.readString();

        AchievementManager.progressAchievement(this.client.getHabbo(), Emulator.getGameEnvironment().getAchievementManager().getAchievement("SafetyQuizGraduate"));
    }
}
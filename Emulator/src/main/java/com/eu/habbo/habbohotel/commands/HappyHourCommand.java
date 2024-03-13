package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.achievements.AchievementManager;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.generic.alerts.GenericAlertComposer;

import java.util.Map;

public class HappyHourCommand extends Command {
    public HappyHourCommand() {
        super("cmd_happyhour", Emulator.getTexts().getValue("commands.keys.cmd_happyhour").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        Emulator.getGameServer().getGameClientManager().sendBroadcastResponse(new GenericAlertComposer("Happy Hour!"));

        for (Map.Entry<Integer, Habbo> set : Emulator.getGameEnvironment().getHabboManager().getOnlineHabbos().entrySet()) {
            AchievementManager.progressAchievement(set.getValue(), Emulator.getGameEnvironment().getAchievementManager().getAchievement("HappyHour"));
        }

        return true;
    }
}

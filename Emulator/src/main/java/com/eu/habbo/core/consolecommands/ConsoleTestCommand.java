package com.eu.habbo.core.consolecommands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConsoleTestCommand extends ConsoleCommand {
    public ConsoleTestCommand() {
        super("test", "This is just a test.");
    }

    @Override
    public void handle(String[] args) throws Exception {
        if (Emulator.debugging) {
            log.info("This is a test command for live debugging.");
            //AchievementManager.progressAchievement(4, Emulator.getGameEnvironment().getAchievementManager().getAchievement("AllTimeHotelPresence"), 30);
            Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(1);
            habbo.getHabboInfo().getMachineID();
        }
    }
}
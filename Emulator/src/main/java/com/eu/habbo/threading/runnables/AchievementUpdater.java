package com.eu.habbo.threading.runnables;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.achievements.Achievement;
import com.eu.habbo.habbohotel.achievements.AchievementManager;
import com.eu.habbo.habbohotel.users.Habbo;

import java.util.Map;

public class AchievementUpdater implements Runnable {
    public static final int INTERVAL = 5 * 60;
    public int lastExecutionTimestamp = Emulator.getIntUnixTimestamp();

    @Override
    public void run() {
        if (!Emulator.isShuttingDown) {
            Emulator.getThreading().run(this, INTERVAL * 1000);
        }

        if (Emulator.isReady) {
            Achievement onlineTime = Emulator.getGameEnvironment().getAchievementManager().getAchievement("AllTimeHotelPresence");

            int timestamp = Emulator.getIntUnixTimestamp();
            for (Map.Entry<Integer, Habbo> set : Emulator.getGameEnvironment().getHabboManager().getOnlineHabbos().entrySet()) {
                int timeOnlineSinceLastInterval = INTERVAL;
                Habbo habbo = set.getValue();
                if (habbo.getHabboInfo().getLastOnline() > this.lastExecutionTimestamp) {
                    timeOnlineSinceLastInterval = timestamp - habbo.getHabboInfo().getLastOnline();
                }
                AchievementManager.progressAchievement(habbo, onlineTime, (int) Math.floor((timeOnlineSinceLastInterval) / 60));
            }

            this.lastExecutionTimestamp = timestamp;
        }
    }
}

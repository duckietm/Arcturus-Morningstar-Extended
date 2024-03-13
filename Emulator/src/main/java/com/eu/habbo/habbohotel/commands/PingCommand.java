package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.habbohotel.LatencyTracker;
import com.eu.habbo.habbohotel.gameclients.GameClient;

public class PingCommand extends Command {
    public PingCommand() {
        super(null, new String[]{"ping"});
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (gameClient.getHabbo().getRoomUnit() == null) {
            return true;
        }

        final LatencyTracker latencyTracker = gameClient.getLatencyTracker();

        if (latencyTracker.hasInitialized()) {
            gameClient.getHabbo().whisper(String.format("Gemiddelde ping %dms, laatste ping %dms",
                    latencyTracker.getAverageMs(),
                    latencyTracker.getLastMs()));
        } else {
            gameClient.getHabbo().whisper("Ping-snelheid is nog niet berekend, probeer het over een minuut opnieuw.");
        }

        return true;
    }
}
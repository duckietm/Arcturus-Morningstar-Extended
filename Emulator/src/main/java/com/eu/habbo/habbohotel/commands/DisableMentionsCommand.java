package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.users.Habbo;

public class DisableMentionsCommand extends Command {
    public DisableMentionsCommand() {
        super("cmd_disablementions", new String[]{"disablementions", "togglementions", "menzioni"});
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (gameClient == null) return true;
        Habbo habbo = gameClient.getHabbo();
        if (habbo == null || habbo.getHabboStats() == null) return true;

        boolean newState = !habbo.getHabboStats().mentionsEnabled();
        habbo.getHabboStats().setMentionsEnabled(newState);

        habbo.whisper(newState
                ? "@mention notifications are now ENABLED for you."
                : "@mention notifications are now DISABLED for you. You will not receive direct or broadcast mentions.");
        return true;
    }
}

package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;

public class SitCommand extends Command {
    public SitCommand() {
        super(null, Emulator.getTexts().getValue("commands.keys.cmd_sit").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (gameClient.getHabbo().getHabboInfo().getRiding() == null) //TODO Make this an event plugin which fires that can be cancelled
            gameClient.getHabbo().getHabboInfo().getCurrentRoom().makeSit(gameClient.getHabbo());
        return true;
    }
}

package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;

public class StandCommand extends Command {
    public StandCommand() {
            super(null, Emulator.getTexts().getValue("commands.keys.cmd_stand").split(";"));
        }


    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (gameClient.getHabbo().getHabboInfo().getRiding() == null)
            gameClient.getHabbo().getHabboInfo().getCurrentRoom().makeStand(gameClient.getHabbo());
        return true;
    }
}

package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;

public class UnloadRoomCommand extends Command {

    public UnloadRoomCommand() {
        super("cmd_unload", Emulator.getTexts().getValue("commands.keys.cmd_unload").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        Room room = gameClient.getHabbo().getHabboInfo().getCurrentRoom();
        if (room != null && room.isOwner(gameClient.getHabbo())) {

            room.dispose();
            return true;
        }
        return false;
    }
}

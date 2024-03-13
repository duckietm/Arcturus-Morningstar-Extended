package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.rooms.ForwardToRoomComposer;

import java.util.ArrayList;
import java.util.Collection;

public class ReloadRoomCommand extends Command {
    public ReloadRoomCommand() {
        super("cmd_reload_room", Emulator.getTexts().getValue("commands.keys.cmd_reload_room").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        Emulator.getThreading().run(() -> {
            Room room = gameClient.getHabbo().getHabboInfo().getCurrentRoom();
            if (room != null) {
                Collection<Habbo> habbos = new ArrayList<>(room.getHabbos());
                Emulator.getGameEnvironment().getRoomManager().unloadRoom(room);
                room = Emulator.getGameEnvironment().getRoomManager().loadRoom(room.getId());
                ServerMessage message = new ForwardToRoomComposer(room.getId()).compose();
                for (Habbo habbo : habbos) {
                    habbo.getClient().sendResponse(message);
                }
            }
        }, 100);

        return true;
    }
}
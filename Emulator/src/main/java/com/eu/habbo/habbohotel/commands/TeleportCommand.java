package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;

public class TeleportCommand extends Command {
    public TeleportCommand() {
        super("cmd_teleport", Emulator.getTexts().getValue("commands.keys.cmd_teleport").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (gameClient.getHabbo().getHabboInfo().getRiding() == null) //TODO Make this an event plugin which fires that can be cancelled
            if (gameClient.getHabbo().getRoomUnit().cmdTeleport) {
                gameClient.getHabbo().getRoomUnit().cmdTeleport = false;
                gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.succes.cmd_teleport.disabled"), RoomChatMessageBubbles.ALERT);
                return true;
            } else {
                gameClient.getHabbo().getRoomUnit().cmdTeleport = true;
                gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.succes.cmd_teleport.enabled"), RoomChatMessageBubbles.ALERT);
                return true;
            }
        return true;
    }
}

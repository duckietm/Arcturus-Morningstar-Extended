package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;

public class SoftKickCommand extends Command {
    public SoftKickCommand() {
        super("cmd_softkick", Emulator.getTexts().getValue("commands.keys.cmd_softkick").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (params.length == 2) {
            final String username = params[1];
            final Habbo habbo = gameClient.getHabbo().getHabboInfo().getCurrentRoom().getHabbo(username);

            if (habbo == null) {
                gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.keys.cmd_softkick_error").replace("%user%", username), RoomChatMessageBubbles.ALERT);
                return true;
            }

            if (habbo == gameClient.getHabbo()) {
                gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.keys.cmd_softkick_error_self"), RoomChatMessageBubbles.ALERT);
                return true;
            }

            final Room room = gameClient.getHabbo().getHabboInfo().getCurrentRoom();

            if (room != null) {
                if (!(habbo.hasPermission(Permission.ACC_UNKICKABLE) || habbo.hasPermission(Permission.ACC_SUPPORTTOOL) || room.isOwner(habbo))) {
                    room.kickHabbo(habbo, false);
                }
            }
        }
        return true;
    }
}

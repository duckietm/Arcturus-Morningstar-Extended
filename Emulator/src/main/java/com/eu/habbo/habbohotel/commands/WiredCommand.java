package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.messages.outgoing.users.InClientLinkComposer;

public class WiredCommand extends Command {
    public WiredCommand() {
        super(Permission.ACC_PLACEFURNI, new String[]{"wired"});
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        Room room = gameClient.getHabbo().getHabboInfo().getCurrentRoom();

        if (room == null) {
            gameClient.getHabbo().whisper("You need to be inside a room to use :wired.", RoomChatMessageBubbles.ALERT);
            return true;
        }

        boolean hasRights = room.hasRights(gameClient.getHabbo())
                || room.isOwner(gameClient.getHabbo())
                || gameClient.getHabbo().hasPermission(Permission.ACC_ANYROOMOWNER);

        if (!hasRights) {
            gameClient.getHabbo().whisper("You need room rights to open the Wired Creator Tools.", RoomChatMessageBubbles.ALERT);
            return true;
        }

        gameClient.sendResponse(new InClientLinkComposer("wired-tools/show"));
        return true;
    }
}

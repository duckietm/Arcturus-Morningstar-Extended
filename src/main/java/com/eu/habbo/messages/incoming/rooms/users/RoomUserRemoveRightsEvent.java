package com.eu.habbo.messages.incoming.rooms.users;

import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.incoming.MessageHandler;

public class RoomUserRemoveRightsEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int amount = this.packet.readInt();

        Room room = this.client.getHabbo().getHabboInfo().getCurrentRoom();

        if (room == null)
            return;

        if (room.getOwnerId() == this.client.getHabbo().getHabboInfo().getId() || this.client.getHabbo().hasPermission(Permission.ACC_ANYROOMOWNER)) {
            for (int i = 0; i < amount; i++) {
                int userId = this.packet.readInt();

                room.removeRights(userId);
            }
        }
    }
}

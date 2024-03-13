package com.eu.habbo.messages.incoming.rooms.users;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserIgnoredComposer;

public class UnIgnoreRoomUserEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        Room room = this.client.getHabbo().getHabboInfo().getCurrentRoom();

        if (room != null) {
            String username = this.packet.readString();

            Habbo habbo = room.getHabbo(username);

            if (habbo != null) {
                if (habbo.getHabboStats().allowTalk()) {
                    this.client.getHabbo().getHabboStats().unignoreUser(habbo.getHabboInfo().getId());
                    this.client.sendResponse(new RoomUserIgnoredComposer(habbo, RoomUserIgnoredComposer.UNIGNORED));
                }
            }
        }
    }
}

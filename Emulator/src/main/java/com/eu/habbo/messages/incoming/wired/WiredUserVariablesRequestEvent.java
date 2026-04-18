package com.eu.habbo.messages.incoming.wired;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.incoming.MessageHandler;

public class WiredUserVariablesRequestEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        Room room = currentRoom();

        if (room == null) {
            return;
        }

        room.getUserVariableManager().sendSnapshot(this.client.getHabbo());
    }

    @Override
    public int getRatelimit() {
        return 50;
    }
}

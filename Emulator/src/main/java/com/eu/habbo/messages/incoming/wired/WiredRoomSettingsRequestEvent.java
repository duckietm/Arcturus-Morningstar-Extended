package com.eu.habbo.messages.incoming.wired;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.wired.WiredRoomSettingsDataComposer;

public class WiredRoomSettingsRequestEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        Room room = currentRoom();

        if (room == null) {
            return;
        }

        this.client.sendResponse(new WiredRoomSettingsDataComposer(room, this.client.getHabbo()));
    }

    @Override
    public int getRatelimit() {
        return 250;
    }
}

package com.eu.habbo.messages.incoming.wired;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.wired.WiredRoomSettingsDataComposer;

public class WiredRoomSettingsSaveEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        Room room = currentRoom();

        if (room == null) {
            return;
        }

        if (this.packet.bytesAvailable() < 8) {
            this.client.sendResponse(new WiredRoomSettingsDataComposer(room, this.client.getHabbo()));
            return;
        }

        if (!room.canManageWiredSettings(this.client.getHabbo())) {
            this.client.sendResponse(new WiredRoomSettingsDataComposer(room, this.client.getHabbo()));
            return;
        }

        int inspectMask = this.packet.readInt();
        int modifyMask = this.packet.readInt();

        room.saveWiredSettings(inspectMask, modifyMask);

        this.client.sendResponse(new WiredRoomSettingsDataComposer(room, this.client.getHabbo()));
    }

    @Override
    public int getRatelimit() {
        return 250;
    }
}

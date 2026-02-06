package com.eu.habbo.messages.incoming.rooms;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.rooms.RoomSettingsComposer;

public class RequestRoomSettingsEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        final int roomId = this.packet.readInt();

        Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(roomId);
        if (room == null) return;
        if (!room.hasRights(this.client.getHabbo()) && !this.client.getHabbo().hasPermission(Permission.ACC_ANYROOMOWNER)) return;

        this.client.sendResponse(new RoomSettingsComposer(room));
    }
}


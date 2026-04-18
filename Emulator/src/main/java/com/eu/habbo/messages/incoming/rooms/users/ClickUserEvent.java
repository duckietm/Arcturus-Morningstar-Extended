package com.eu.habbo.messages.incoming.rooms.users;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.items.interactions.wired.triggers.WiredTriggerHabboClicksUser;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.users.InClientLinkComposer;

public class ClickUserEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        Room room = this.client.getHabbo().getHabboInfo().getCurrentRoom();

        if (room == null) {
            return;
        }

        RoomUnit clickingUser = this.client.getHabbo().getRoomUnit();

        if (clickingUser == null) {
            return;
        }

        int roomUnitId = this.packet.readInt();
        Habbo clickedHabbo = room.getHabboByRoomUnitId(roomUnitId);

        if (clickedHabbo == null || clickedHabbo.getRoomUnit() == null) {
            return;
        }

        WiredManager.triggerUserClicksUser(room, clickingUser, clickedHabbo.getRoomUnit());

        if (WiredTriggerHabboClicksUser.hasPendingIgnoreLook(clickingUser)) {
            this.client.sendResponse(new InClientLinkComposer("avatar-info/block-rotate"));
        }

        if (WiredTriggerHabboClicksUser.consumeBlockMenuOpen(clickingUser)) {
            this.client.sendResponse(new InClientLinkComposer("avatar-info/block-menu"));
        }
    }
}

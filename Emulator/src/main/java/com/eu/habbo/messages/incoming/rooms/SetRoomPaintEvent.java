package com.eu.habbo.messages.incoming.rooms;

import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.rooms.RoomPaintComposer;

/**
 * :modificastanza — apply floor / wallpaper / landscape paint directly, without a consumable
 * paint item. Same gate as placing a bought paint (owner, rights holders, ACC_PLACEFURNI);
 * values are validated against the classic paint code shapes ("0.0" = room default).
 */
public class SetRoomPaintEvent extends MessageHandler {

    @Override
    public int getRatelimit() {
        return 250;
    }

    @Override
    public void handle() throws Exception {
        Room room = currentRoom();
        if (room == null) return;

        String type = this.packet.readString();
        String value = this.packet.readString();
        if (type == null || value == null) return;
        type = type.trim();
        value = value.trim();
        if (value.isEmpty() || value.length() > 8) return;

        boolean allowed = room.getOwnerId() == this.client.getHabbo().getHabboInfo().getId()
                || room.hasRights(this.client.getHabbo())
                || this.client.getHabbo().hasPermission(Permission.ACC_PLACEFURNI);
        if (!allowed) return;

        boolean isDefault = value.equals("0.0");
        switch (type) {
            case "floor":
                if (!isDefault && !value.matches("\\d{3,5}")) return;
                room.setFloorPaint(value);
                break;
            case "wallpaper":
                if (!isDefault && !value.matches("\\d{3,5}")) return;
                room.setWallPaint(value);
                break;
            case "landscape":
                if (!isDefault && !value.matches("\\d{1,2}\\.\\d{1,2}")) return;
                room.setBackgroundPaint(value);
                break;
            default:
                return;
        }

        room.setNeedsUpdate(true);
        room.sendComposer(new RoomPaintComposer(type, value).compose());
    }
}

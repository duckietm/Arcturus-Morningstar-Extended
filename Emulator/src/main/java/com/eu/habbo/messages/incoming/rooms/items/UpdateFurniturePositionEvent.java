package com.eu.habbo.messages.incoming.rooms.items;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.rooms.items.FloorItemUpdateComposer;

public class UpdateFurniturePositionEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        Room room = this.client.getHabbo().getHabboInfo().getCurrentRoom();

        if (room == null) return;

        int furniId = this.packet.readInt();
        HabboItem item = room.getHabboItem(furniId);
        if (item == null) return;

        int x = this.packet.readInt();
        int y = this.packet.readInt();
        double z = (double) this.packet.readInt() / 10000;
        int rotation = this.packet.readInt();
        RoomTile tile = room.getLayout().getTile((short) x, (short) y);

        room.moveFurniTo(item, tile, rotation, z, this.client.getHabbo(), true, true);
        this.client.sendResponse(new FloorItemUpdateComposer(item));
    }
}
package com.eu.habbo.threading.runnables;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.outgoing.rooms.UpdateStackHeightComposer;
import com.eu.habbo.messages.outgoing.rooms.items.RemoveFloorItemComposer;

class RemoveFloorItemTask implements Runnable {
    private final Room room;
    private final HabboItem item;

    public RemoveFloorItemTask(Room room, HabboItem item) {
        this.room = room;
        this.item = item;
    }

    @Override
    public void run() {
        if (this.item == null || this.room == null)
            return;

        RoomTile tile = this.room.getLayout().getTile(this.item.getX(), this.item.getY());
        this.room.removeHabboItem(this.item);
        this.room.updateTile(tile);
        this.room.sendComposer(new RemoveFloorItemComposer(this.item, true).compose());
        this.room.sendComposer(new UpdateStackHeightComposer(this.item.getX(), this.item.getY(), tile.z, tile.relativeHeight()).compose());
    }
}

package com.eu.habbo.threading.runnables;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;

public class RoomUnitGiveHanditem implements Runnable {
    private final RoomUnit roomUnit;
    private final Room room;
    private final int itemId;

    public RoomUnitGiveHanditem(RoomUnit roomUnit, Room room, int itemId) {
        this.roomUnit = roomUnit;
        this.room = room;
        this.itemId = itemId;
    }

    @Override
    public void run() {
        if (this.room != null && this.roomUnit.isInRoom()) {
            this.room.giveHandItem(this.roomUnit, this.itemId);
        }
    }
}

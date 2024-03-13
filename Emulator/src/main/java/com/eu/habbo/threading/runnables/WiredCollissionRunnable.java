package com.eu.habbo.threading.runnables;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;

public class WiredCollissionRunnable implements Runnable {
    public final RoomUnit roomUnit;
    public final Room room;
    public final Object[] objects;

    public WiredCollissionRunnable(RoomUnit roomUnit, Room room, Object[] objects) {
        this.roomUnit = roomUnit;
        this.room = room;
        this.objects = objects;
    }


    @Override
    public void run() {
        WiredHandler.handle(WiredTriggerType.COLLISION, roomUnit, room, objects);
    }
}

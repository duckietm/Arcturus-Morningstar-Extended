package com.eu.habbo.threading.runnables;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.wired.core.WiredManager;

public class WiredCollissionRunnable implements Runnable {
    public final RoomUnit roomUnit;
    public final Room room;

    public WiredCollissionRunnable(RoomUnit roomUnit, Room room) {
        this.roomUnit = roomUnit;
        this.room = room;
    }


    @Override
    public void run() {
        if (this.roomUnit == null || this.room == null || !this.room.isLoaded()) return;
        try {
            WiredManager.triggerBotCollision(room, roomUnit);
        } catch (Exception e) {
            // Prevent task from crashing the thread pool
        }
    }
}

package com.eu.habbo.threading.runnables;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserEffectComposer;

public class SendRoomUnitEffectComposer implements Runnable {
    private final Room room;
    private final RoomUnit roomUnit;

    public SendRoomUnitEffectComposer(Room room, RoomUnit roomUnit) {
        this.room = room;
        this.roomUnit = roomUnit;
    }

    @Override
    public void run() {
        if (this.room != null && this.roomUnit != null) {
            this.room.sendComposer(new RoomUserEffectComposer(roomUnit).compose());
        }
    }
}
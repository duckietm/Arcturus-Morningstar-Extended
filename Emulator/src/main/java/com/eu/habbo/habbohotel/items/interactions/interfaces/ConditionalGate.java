package com.eu.habbo.habbohotel.items.interactions.interfaces;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;

public interface ConditionalGate {
    public void onRejected(RoomUnit roomUnit, Room room, Object[] objects);
}

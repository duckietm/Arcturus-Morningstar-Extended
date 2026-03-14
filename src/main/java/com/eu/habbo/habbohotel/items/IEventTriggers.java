package com.eu.habbo.habbohotel.items;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;

public interface IEventTriggers {
    void onClick(GameClient client, Room room, Object[] objects) throws Exception;

    void onWalkOn(RoomUnit roomUnit, Room room, Object[] objects) throws Exception;

    void onWalkOff(RoomUnit roomUnit, Room room, Object[] objects) throws Exception;
}

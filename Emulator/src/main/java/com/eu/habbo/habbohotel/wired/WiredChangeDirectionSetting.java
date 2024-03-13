package com.eu.habbo.habbohotel.wired;

import com.eu.habbo.habbohotel.rooms.RoomUserRotation;

public class WiredChangeDirectionSetting {
    public final int item_id;
    public int rotation;
    public RoomUserRotation direction;

    public WiredChangeDirectionSetting(int itemId, int rotation, RoomUserRotation direction) {
        this.item_id = itemId;
        this.rotation = rotation;
        this.direction = direction;
    }
}

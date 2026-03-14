package com.eu.habbo.threading.runnables;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoomUnitTeleportWalkToAction implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoomUnitTeleportWalkToAction.class);

    private final Habbo habbo;
    private final HabboItem habboItem;
    private final Room room;

    public RoomUnitTeleportWalkToAction(Habbo habbo, HabboItem habboItem, Room room) {
        this.habbo = habbo;
        this.habboItem = habboItem;
        this.room = room;
    }

    @Override
    public void run() {
        if (this.habbo.getHabboInfo().getCurrentRoom() == this.room) {
            if (this.habboItem.getRoomId() == this.room.getId()) {
                RoomTile tile = HabboItem.getSquareInFront(this.room.getLayout(), this.habboItem);

                if (tile != null) {
                    if (this.habbo.getRoomUnit().getGoal().equals(tile)) {
                        if (this.habbo.getRoomUnit().getCurrentLocation().equals(tile)) {
                            try {
                                this.habboItem.onClick(this.habbo.getClient(), this.room, new Object[]{0});
                            } catch (Exception e) {
                                LOGGER.error("Caught exception", e);
                            }
                        } else {
                            if (tile.isWalkable()) {
                                this.habbo.getRoomUnit().setGoalLocation(tile);
                                Emulator.getThreading().run(this, this.habbo.getRoomUnit().getPath().size() + 2 * 510);
                            }
                        }
                    }
                }
            }
        }
    }
}

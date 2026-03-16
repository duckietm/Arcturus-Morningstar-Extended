package com.eu.habbo.habbohotel.items.interactions.games.football;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionDefault;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.threading.runnables.RebugKickBallAction;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Rebug-style football interaction.
 * Uses simplified momentum-decay physics with 180-degree bounce.
 * Set interaction_type to "rebug_football" on a ball item to use this instead of the default football physics.
 */
public class InteractionRebugFootball extends InteractionDefault {

    private RebugKickBallAction currentThread;

    public InteractionRebugFootball(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        this.setExtradata("0");
    }

    public InteractionRebugFootball(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
        this.setExtradata("0");
    }

    @Override
    public boolean canWalkOn(RoomUnit roomUnit, Room room, Object[] objects) {
        return true;
    }

    @Override
    public boolean isWalkable() {
        return true;
    }

    @Override
    public void onWalkOn(RoomUnit roomUnit, Room room, Object[] objects) throws Exception {
        super.onWalkOn(roomUnit, room, objects);

        if (this.currentThread != null) {
            this.currentThread.dead = true;
        }

        boolean hasPath = !roomUnit.getPath().isEmpty();
        this.currentThread = new RebugKickBallAction(this, room, roomUnit, hasPath);
        Emulator.getThreading().run(this.currentThread, 50);
    }
}

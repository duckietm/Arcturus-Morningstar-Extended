package com.eu.habbo.habbohotel.items.interactions.games.football;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionDefault;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomLayout;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.threading.runnables.RebugKickBallAction;
import com.eu.habbo.util.pathfinding.Direction8;

import java.sql.ResultSet;
import java.sql.SQLException;

public class InteractionRebugFootball extends InteractionDefault {

    private RebugKickBallAction currentThread;
    private Direction8 lastDribbleDirection;

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

        Direction8 userDir = Direction8.getDirection(roomUnit.getBodyRotation().getValue());
        this.lastDribbleDirection = userDir;

        // If this tile is the user's final destination, they'll stop here → long shot
        RoomTile goal = roomUnit.getGoal();
        if (goal != null && goal.x == this.getX() && goal.y == this.getY()) {
            this.kick(room, roomUnit, 55);
        } else {
            // Dribble: ball moves 1 tile ahead of the user
            this.kick(room, roomUnit, 0);
        }
    }

    @Override
    public void onWalkOff(RoomUnit roomUnit, Room room, Object[] objects) throws Exception {
        super.onWalkOff(roomUnit, room, objects);

        if (objects != null && objects.length >= 2 && objects[1] instanceof RoomTile && objects[0] instanceof RoomTile) {
            RoomTile fromTile = (RoomTile) objects[0];
            RoomTile nextTile = (RoomTile) objects[1];

            int dx = nextTile.x - fromTile.x;
            int dy = nextTile.y - fromTile.y;
            Direction8 walkDir = Direction8.fromDelta(dx, dy);

            // User is walking in the same direction as the ball was dribbled → long shot
            if (this.lastDribbleDirection != null && walkDir.getRot() == this.lastDribbleDirection.getRot()) {
                this.kick(room, roomUnit, 55);
                return;
            }
        }

        // Walking sideways or away → just stop the ball
        if (this.currentThread != null) {
            this.currentThread.dead = true;
            this.currentThread = null;
        }
    }

    @Override
    public void onClick(GameClient client, Room room, Object[] objects) throws Exception {
        super.onClick(client, room, objects);

        if (client == null) return;
        RoomUnit unit = client.getHabbo().getRoomUnit();
        if (RoomLayout.tilesAdjecent(unit.getCurrentLocation(), room.getLayout().getTile(this.getX(), this.getY()))) {
            // Long shot when clicking the ball
            this.kick(room, unit, 55);
        }
    }

    private void kick(Room room, RoomUnit kicker, int momentum) {
        if (this.currentThread != null) {
            this.currentThread.dead = true;
        }

        Direction8 direction = Direction8.getDirection(kicker.getBodyRotation().getValue());
        this.currentThread = new RebugKickBallAction(this, room, direction, momentum);
        Emulator.getThreading().run(this.currentThread, 50);
    }
}

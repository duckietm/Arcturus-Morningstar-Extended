package com.eu.habbo.threading.runnables;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomTileState;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.outgoing.rooms.items.FloorItemOnRollerComposer;
import com.eu.habbo.util.pathfinding.Direction8;
import gnu.trove.set.hash.THashSet;

/**
 * Alternative football physics based on the Rebug plugin.
 * Uses momentum decay (ball slows down over time) and simple 180-degree bounce.
 */
public class RebugKickBallAction implements Runnable {

    private final HabboItem ball;
    private final Room room;
    private Direction8 direction;
    private int momentum;
    public boolean dead = false;

    public RebugKickBallAction(HabboItem ball, Room room, RoomUnit kicker, boolean hasPath) {
        this.ball = ball;
        this.room = room;
        this.direction = Direction8.fromDelta(
                ball.getX() - kicker.getX(),
                ball.getY() - kicker.getY()
        );
        this.momentum = hasPath ? 55 : 0;
    }

    private boolean isTileBlocked(int x, int y) {
        RoomTile tile = this.room.getLayout().getTile((short) x, (short) y);
        if (tile == null) return true;
        if (tile.hasUnits()) return true;
        return tile.getState() != RoomTileState.OPEN;
    }

    @Override
    public void run() {
        if (this.dead || !this.room.isLoaded()) return;

        try {
            int nextX = this.ball.getX() + this.direction.getDiffX();
            int nextY = this.ball.getY() + this.direction.getDiffY();

            if (isTileBlocked(nextX, nextY)) {
                this.direction = this.direction.rotateDirection180Degrees();
                nextX = this.ball.getX() + this.direction.getDiffX();
                nextY = this.ball.getY() + this.direction.getDiffY();
            }

            RoomTile nextTile = this.room.getLayout().getTile((short) nextX, (short) nextY);
            if (nextTile == null) return;

            RoomTile oldTile = this.room.getLayout().getTile(this.ball.getX(), this.ball.getY());
            double oldZ = this.ball.getZ();

            this.ball.setRotation(this.direction.getRot());
            this.ball.setX(nextTile.x);
            this.ball.setY(nextTile.y);
            this.ball.setZ(nextTile.getStackHeight());
            this.ball.needsUpdate(true);

            // Schedule next movement based on momentum
            long delay = getDelayForMomentum(this.momentum);
            if (delay > 0) {
                Emulator.getThreading().run(this, delay);
            }

            // Update tiles
            this.room.updateTile(oldTile);
            this.room.updateTile(nextTile);

            THashSet<HabboItem> oldItems = this.room.getItemsAt(oldTile);
            if (oldItems != null && !oldItems.isEmpty()) {
                oldItems.remove(this.ball);
            }
            this.room.getItemsAt(nextTile).add(this.ball);

            // Send rolling animation
            this.room.sendComposer(new FloorItemOnRollerComposer(
                    this.ball, null, oldTile, oldZ, nextTile, this.ball.getZ(), 0.0D, this.room
            ).compose());

            // Decay momentum
            this.momentum -= 11;
        } catch (Exception e) {
            this.dead = true;
        }
    }

    private long getDelayForMomentum(int momentum) {
        switch (momentum) {
            case 55: return 100L;
            case 44: return 100L;
            case 33: return 200L;
            case 22: return 250L;
            case 11: return 500L;
            default: return 0L;
        }
    }
}

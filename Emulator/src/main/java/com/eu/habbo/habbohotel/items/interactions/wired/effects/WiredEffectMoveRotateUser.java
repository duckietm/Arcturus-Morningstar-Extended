package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomTileState;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.rooms.RoomUserRotation;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.habbohotel.wired.core.WiredMoveCarryHelper;
import com.eu.habbo.habbohotel.wired.core.WiredSourceUtil;
import com.eu.habbo.habbohotel.wired.core.WiredUserMovementHelper;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserStatusComposer;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import gnu.trove.procedure.TObjectProcedure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WiredEffectMoveRotateUser extends InteractionWiredEffect {
    private static final int ROTATION_CLOCKWISE = 8;
    private static final int ROTATION_COUNTER_CLOCKWISE = 9;
    private static final String CACHE_ACTIVE_UNTIL = "wired.move_rotate_user.active_until";
    private static final String CACHE_WALK_IN_PLACE_UNTIL = "wired.move_rotate_user.walk_in_place_until";
    private static final int WALK_IN_PLACE_DURATION_MS = 550;
    private static final int ROTATION_ACTIVE_WINDOW_MS = 250;

    public static final WiredEffectType type = WiredEffectType.MOVE_ROTATE_USER;

    private int movementDirection = -1;
    private int rotationDirection = -1;
    private int userSource = WiredSourceUtil.SOURCE_TRIGGER;

    public WiredEffectMoveRotateUser(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectMoveRotateUser(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public void execute(WiredContext ctx) {
        Room room = ctx.room();

        for (RoomUnit roomUnit : WiredSourceUtil.resolveUsers(ctx, this.userSource)) {
            if (roomUnit == null || roomUnit.getRoom() != room) {
                continue;
            }

            boolean hasRotation = this.rotationDirection >= 0;
            RoomUserRotation targetBodyRotation = hasRotation ? this.getTargetRotation(roomUnit) : roomUnit.getBodyRotation();
            RoomUserRotation targetHeadRotation = hasRotation ? targetBodyRotation : roomUnit.getHeadRotation();
            RoomTile targetTile = (this.movementDirection >= 0) ? this.getTargetTile(room, roomUnit, this.movementDirection) : null;
            boolean canMove = this.canMoveTo(room, roomUnit, targetTile);
            boolean noAnimation = WiredMoveCarryHelper.hasNoAnimationExtra(room, this);
            int animationDuration = noAnimation ? 0 : WiredMoveCarryHelper.getAnimationDuration(room, this, WiredUserMovementHelper.DEFAULT_ANIMATION_DURATION);
            int activeWindowMs = this.resolveActiveWindow(canMove, hasRotation, noAnimation, animationDuration);

            if (canMove) {
                double targetZ = targetTile.getStackHeight() + ((targetTile.state == RoomTileState.SIT) ? -0.5 : 0);
                this.markActive(roomUnit, activeWindowMs);
                if (!WiredUserMovementHelper.moveUser(room, roomUnit, targetTile, targetZ, targetBodyRotation, targetHeadRotation,
                        animationDuration, noAnimation)) {
                    if (hasRotation) {
                        WiredUserMovementHelper.updateUserDirection(room, roomUnit, targetBodyRotation, targetHeadRotation);
                    }
                }
                continue;
            }

            if (hasRotation) {
                this.markActive(roomUnit, activeWindowMs);
                WiredUserMovementHelper.updateUserDirection(room, roomUnit, targetBodyRotation, targetHeadRotation);
            }
        }
    }

    @Override
    public String getWiredData() {
        return WiredManager.getGson().toJson(new JsonData(
            this.getDelay(),
            this.movementDirection,
            this.rotationDirection,
            this.userSource
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");

        if (wiredData.startsWith("{")) {
            JsonData data = WiredManager.getGson().fromJson(wiredData, JsonData.class);
            this.setDelay(data.delay);
            this.movementDirection = this.normalizeDirection(data.movementDirection);
            this.rotationDirection = this.normalizeRotation(data.rotationDirection);
            this.userSource = data.userSource;
            return;
        }

        this.setDelay(0);
        this.movementDirection = -1;
        this.rotationDirection = -1;
        this.userSource = WiredSourceUtil.SOURCE_TRIGGER;
    }

    @Override
    public void onPickUp() {
        this.setDelay(0);
        this.movementDirection = -1;
        this.rotationDirection = -1;
        this.userSource = WiredSourceUtil.SOURCE_TRIGGER;
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(5);
        message.appendInt(0);
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(3);
        message.appendInt(this.movementDirection);
        message.appendInt(this.rotationDirection);
        message.appendInt(this.userSource);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());

        if (this.requiresTriggeringUser()) {
            List<Integer> invalidTriggers = new ArrayList<>();
            room.getRoomSpecialTypes().getTriggers(this.getX(), this.getY()).forEach(new TObjectProcedure<InteractionWiredTrigger>() {
                @Override
                public boolean execute(InteractionWiredTrigger object) {
                    if (!object.isTriggeredByRoomUnit()) {
                        invalidTriggers.add(object.getBaseItem().getSpriteId());
                    }
                    return true;
                }
            });
            message.appendInt(invalidTriggers.size());
            for (Integer i : invalidTriggers) {
                message.appendInt(i);
            }
        } else {
            message.appendInt(0);
        }
    }

    @Override
    public boolean saveData(WiredSettings settings, GameClient gameClient) throws WiredSaveException {
        if (settings.getIntParams().length < 3) {
            throw new WiredSaveException("Invalid data");
        }

        int delay = settings.getDelay();
        if (delay > Emulator.getConfig().getInt("hotel.wired.max_delay", 20)) {
            throw new WiredSaveException("Delay too long");
        }

        this.movementDirection = this.normalizeDirection(settings.getIntParams()[0]);
        this.rotationDirection = this.normalizeRotation(settings.getIntParams()[1]);
        this.userSource = settings.getIntParams()[2];
        this.setDelay(delay);

        return true;
    }

    @Override
    public boolean requiresTriggeringUser() {
        return this.userSource == WiredSourceUtil.SOURCE_TRIGGER;
    }

    @Override
    protected long requiredCooldown() {
        return COOLDOWN_MOVEMENT;
    }

    @Deprecated
    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return false;
    }

    private int normalizeDirection(int direction) {
        return (direction >= 0 && direction <= 7) ? direction : -1;
    }

    private int normalizeRotation(int rotation) {
        return ((rotation >= 0 && rotation <= 7) || rotation == ROTATION_CLOCKWISE || rotation == ROTATION_COUNTER_CLOCKWISE) ? rotation : -1;
    }

    private RoomUserRotation getTargetRotation(RoomUnit roomUnit) {
        RoomUserRotation currentRotation = (roomUnit != null && roomUnit.getBodyRotation() != null) ? roomUnit.getBodyRotation() : RoomUserRotation.NORTH;

        if (this.rotationDirection == ROTATION_CLOCKWISE) {
            return RoomUserRotation.clockwise(currentRotation);
        }

        if (this.rotationDirection == ROTATION_COUNTER_CLOCKWISE) {
            return RoomUserRotation.counterClockwise(currentRotation);
        }

        return RoomUserRotation.fromValue(this.rotationDirection);
    }

    private RoomTile getTargetTile(Room room, RoomUnit roomUnit, int direction) {
        RoomTile currentTile = roomUnit.getCurrentLocation();

        if (currentTile == null) {
            return null;
        }

        int deltaX = 0;
        int deltaY = 0;

        switch (RoomUserRotation.fromValue(direction)) {
            case NORTH:
                deltaY = 1;
                break;
            case NORTH_EAST:
                deltaX = 1;
                deltaY = 1;
                break;
            case EAST:
                deltaX = 1;
                break;
            case SOUTH_EAST:
                deltaX = 1;
                deltaY = -1;
                break;
            case SOUTH:
                deltaY = -1;
                break;
            case SOUTH_WEST:
                deltaX = -1;
                deltaY = -1;
                break;
            case WEST:
                deltaX = -1;
                break;
            case NORTH_WEST:
                deltaX = -1;
                deltaY = 1;
                break;
        }

        return room.getLayout().getTile((short) (currentTile.x + deltaX), (short) (currentTile.y + deltaY));
    }

    private boolean canMoveTo(Room room, RoomUnit roomUnit, RoomTile targetTile) {
        if (targetTile == null || targetTile.state == RoomTileState.INVALID || targetTile.state == RoomTileState.BLOCKED) {
            return false;
        }

        if (!room.tileWalkable(targetTile)) {
            return false;
        }

        for (RoomUnit unit : room.getRoomUnitsAt(targetTile)) {
            if (unit != null && unit != roomUnit) {
                return false;
            }
        }

        return true;
    }

    private void markActive(RoomUnit roomUnit, int durationMs) {
        if (roomUnit == null || durationMs <= 0) {
            return;
        }

        long activeUntil = System.currentTimeMillis() + durationMs;
        roomUnit.getCacheable().put(CACHE_ACTIVE_UNTIL, activeUntil);
    }

    private int resolveActiveWindow(boolean canMove, boolean hasRotation, boolean noAnimation, int animationDuration) {
        if (noAnimation) {
            return 0;
        }

        if (canMove) {
            return Math.max(1, animationDuration);
        }

        if (hasRotation) {
            return ROTATION_ACTIVE_WINDOW_MS;
        }

        return 0;
    }

    public static boolean handleWalkWhileActive(Room room, RoomUnit roomUnit, RoomTile targetTile) {
        if (room == null || roomUnit == null || !isActive(roomUnit)) {
            return false;
        }

        long walkInPlaceUntil = System.currentTimeMillis() + WALK_IN_PLACE_DURATION_MS;
        roomUnit.getCacheable().put(CACHE_WALK_IN_PLACE_UNTIL, walkInPlaceUntil);
        roomUnit.stopWalking();
        roomUnit.removeStatus(RoomUnitStatus.MOVE);
        roomUnit.setStatus(RoomUnitStatus.MOVE, roomUnit.getX() + "," + roomUnit.getY() + "," + roomUnit.getZ());
        roomUnit.statusUpdate(false);
        room.sendComposer(new RoomUserStatusComposer(roomUnit).compose());

        Emulator.getThreading().run(() -> clearWalkInPlace(room, roomUnit, walkInPlaceUntil), WALK_IN_PLACE_DURATION_MS);
        return true;
    }

    private static boolean isActive(RoomUnit roomUnit) {
        Long activeUntil = getCachedTimestamp(roomUnit, CACHE_ACTIVE_UNTIL);
        return activeUntil != null && activeUntil > System.currentTimeMillis();
    }

    private static boolean isWalkInPlaceActive(RoomUnit roomUnit) {
        Long walkInPlaceUntil = getCachedTimestamp(roomUnit, CACHE_WALK_IN_PLACE_UNTIL);

        if (walkInPlaceUntil == null) {
            return false;
        }

        if (walkInPlaceUntil <= System.currentTimeMillis()) {
            roomUnit.getCacheable().remove(CACHE_WALK_IN_PLACE_UNTIL);
            return false;
        }

        return true;
    }

    private static Long getCachedTimestamp(RoomUnit roomUnit, String key) {
        if (roomUnit == null || key == null) {
            return null;
        }

        Object value = roomUnit.getCacheable().get(key);

        if (value instanceof Long) {
            return (Long) value;
        }

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        return null;
    }

    private static void clearWalkInPlace(Room room, RoomUnit roomUnit, long expectedUntil) {
        if (room == null || roomUnit == null || !room.isLoaded()) {
            return;
        }

        Long currentUntil = getCachedTimestamp(roomUnit, CACHE_WALK_IN_PLACE_UNTIL);
        if (currentUntil == null || currentUntil.longValue() != expectedUntil) {
            return;
        }

        roomUnit.getCacheable().remove(CACHE_WALK_IN_PLACE_UNTIL);

        if (roomUnit.hasStatus(RoomUnitStatus.MOVE) && !roomUnit.isWalking()) {
            roomUnit.removeStatus(RoomUnitStatus.MOVE);
            roomUnit.statusUpdate(false);
            room.sendComposer(new RoomUserStatusComposer(roomUnit).compose());
        }
    }

    static class JsonData {
        int delay;
        int movementDirection;
        int rotationDirection;
        int userSource;

        public JsonData(int delay, int movementDirection, int rotationDirection, int userSource) {
            this.delay = delay;
            this.movementDirection = movementDirection;
            this.rotationDirection = rotationDirection;
            this.userSource = userSource;
        }
    }
}

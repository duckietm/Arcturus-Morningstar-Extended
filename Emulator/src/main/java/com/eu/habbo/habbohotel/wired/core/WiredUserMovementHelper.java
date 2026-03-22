package com.eu.habbo.habbohotel.wired.core;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.interactions.InteractionRoller;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.rooms.RoomUserRotation;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.outgoing.rooms.WiredMovementsComposer;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserStatusComposer;
import gnu.trove.set.hash.THashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class WiredUserMovementHelper {
    public static final int DEFAULT_ANIMATION_DURATION = WiredMovementsComposer.DEFAULT_DURATION;
    private static final int STATUS_SUPPRESSION_GRACE_MS = 250;

    private static final Logger LOGGER = LoggerFactory.getLogger(WiredUserMovementHelper.class);
    private static final ThreadLocal<Set<Integer>> SUPPRESSED_STATUS_ROOM_UNIT_IDS = new ThreadLocal<>();
    private static final ConcurrentHashMap<Integer, Long> SUPPRESSED_STATUS_COMPOSER_UNTIL = new ConcurrentHashMap<>();

    private WiredUserMovementHelper() {
    }

    public static boolean moveUser(Room room, RoomUnit roomUnit, RoomTile targetTile, double targetZ, int duration) {
        return moveUser(room, roomUnit, targetTile, targetZ, roomUnit == null ? null : roomUnit.getBodyRotation(),
                roomUnit == null ? null : roomUnit.getHeadRotation(), duration, false);
    }

    public static boolean moveUser(Room room, RoomUnit roomUnit, RoomTile targetTile, double targetZ, RoomUserRotation bodyRotation, RoomUserRotation headRotation, int duration) {
        return moveUser(room, roomUnit, targetTile, targetZ, bodyRotation, headRotation, duration, false);
    }

    public static boolean moveUser(Room room, RoomUnit roomUnit, RoomTile targetTile, double targetZ, RoomUserRotation bodyRotation, RoomUserRotation headRotation, int duration, boolean noAnimation) {
        if (room == null || roomUnit == null || targetTile == null || room.getLayout() == null) {
            return false;
        }

        RoomTile oldLocation = roomUnit.getCurrentLocation();
        if (oldLocation == null || hasBlockingUnits(room, roomUnit, targetTile)) {
            return false;
        }

        RoomUserRotation resolvedBodyRotation = bodyRotation == null ? roomUnit.getBodyRotation() : bodyRotation;
        RoomUserRotation resolvedHeadRotation = headRotation == null ? roomUnit.getHeadRotation() : headRotation;
        double oldZ = roomUnit.getZ();
        HabboItem oldTopItem = room.getTopItemAt(oldLocation.x, oldLocation.y);
        HabboItem newTopItem = room.getTopItemAt(targetTile.x, targetTile.y);
        Habbo habbo = room.getHabbo(roomUnit);
        int animationDuration = Math.max(1, duration);

        if (noAnimation) {
            return moveUserInstant(room, roomUnit, targetTile, targetZ, resolvedBodyRotation, resolvedHeadRotation, oldLocation, oldTopItem, newTopItem, habbo);
        }

        runWithSuppressedStatusUpdates(Collections.singletonList(roomUnit), () -> {
            roomUnit.setPreviousLocation(oldLocation);
            roomUnit.setCurrentLocation(targetTile);
            roomUnit.removeStatus(RoomUnitStatus.MOVE);
            roomUnit.setZ(targetZ);
            roomUnit.setBodyRotation(resolvedBodyRotation);
            roomUnit.setHeadRotation(resolvedHeadRotation);
            roomUnit.stopWalking();
            roomUnit.resetIdleTimer();

            if (habbo != null) {
                THashSet<Habbo> movedHabbos = new THashSet<>();
                movedHabbos.add(habbo);
                room.updateHabbosAt(targetTile.x, targetTile.y, movedHabbos);
            }

            roomUnit.statusUpdate(false);
        });

        List<WiredMovementsComposer.MovementData> movements = new ArrayList<>();
        movements.add(WiredMovementsComposer.userSlideMovement(
                roomUnit.getId(),
                oldLocation.x,
                oldLocation.y,
                targetTile.x,
                targetTile.y,
                oldZ,
                roomUnit.getZ(),
                resolvedBodyRotation.getValue(),
                resolvedHeadRotation.getValue(),
                animationDuration));
        suppressStatusComposer(roomUnit, animationDuration);
        room.sendComposer(new WiredMovementsComposer(movements).compose());

        roomUnit.setPreviousLocation(targetTile);
        roomUnit.setPreviousLocationZ(roomUnit.getZ());

        scheduleTileCallbacks(room, roomUnit, oldLocation, targetTile, oldTopItem, newTopItem, animationDuration);
        schedulePostureSync(room, roomUnit, targetTile, animationDuration);
        return true;
    }

    public static boolean updateUserDirection(Room room, RoomUnit roomUnit, RoomUserRotation bodyRotation, RoomUserRotation headRotation) {
        if (room == null || roomUnit == null) {
            return false;
        }

        RoomUserRotation resolvedBodyRotation = bodyRotation == null ? roomUnit.getBodyRotation() : bodyRotation;
        RoomUserRotation resolvedHeadRotation = headRotation == null ? roomUnit.getHeadRotation() : headRotation;

        roomUnit.setBodyRotation(resolvedBodyRotation);
        roomUnit.setHeadRotation(resolvedHeadRotation);
        room.sendComposer(new WiredMovementsComposer(Collections.singletonList(
                WiredMovementsComposer.userDirectionUpdate(
                        roomUnit.getId(),
                        resolvedHeadRotation.getValue(),
                        resolvedBodyRotation.getValue()))).compose());
        return true;
    }

    public static boolean shouldSuppressStatusUpdate(RoomUnit roomUnit) {
        if (roomUnit == null) {
            return false;
        }

        Set<Integer> suppressedRoomUnitIds = SUPPRESSED_STATUS_ROOM_UNIT_IDS.get();
        return suppressedRoomUnitIds != null && suppressedRoomUnitIds.contains(roomUnit.getId());
    }

    public static boolean shouldSuppressStatusComposer(RoomUnit roomUnit) {
        if (roomUnit == null) {
            return false;
        }

        Long suppressedUntil = SUPPRESSED_STATUS_COMPOSER_UNTIL.get(roomUnit.getId());

        if (suppressedUntil == null) {
            return false;
        }

        if (suppressedUntil <= System.currentTimeMillis()) {
            SUPPRESSED_STATUS_COMPOSER_UNTIL.remove(roomUnit.getId(), suppressedUntil);
            return false;
        }

        return true;
    }

    public static void clearStatusComposerSuppression(RoomUnit roomUnit) {
        if (roomUnit == null) {
            return;
        }

        SUPPRESSED_STATUS_COMPOSER_UNTIL.remove(roomUnit.getId());
    }

    private static boolean hasBlockingUnits(Room room, RoomUnit roomUnit, RoomTile targetTile) {
        Collection<RoomUnit> units = room.getRoomUnitsAt(targetTile);

        if (units == null || units.isEmpty()) {
            return false;
        }

        for (RoomUnit targetUnit : units) {
            if (targetUnit != null && targetUnit != roomUnit) {
                return true;
            }
        }

        return false;
    }

    private static boolean moveUserInstant(Room room, RoomUnit roomUnit, RoomTile targetTile, double targetZ, RoomUserRotation bodyRotation, RoomUserRotation headRotation, RoomTile oldLocation, HabboItem oldTopItem, HabboItem newTopItem, Habbo habbo) {
        runWithSuppressedStatusUpdates(Collections.singletonList(roomUnit), () -> {
            roomUnit.setPreviousLocation(oldLocation);
            roomUnit.setCurrentLocation(targetTile);
            roomUnit.removeStatus(RoomUnitStatus.MOVE);
            roomUnit.setZ(targetZ);
            roomUnit.setBodyRotation(bodyRotation);
            roomUnit.setHeadRotation(headRotation);
            roomUnit.stopWalking();
            roomUnit.resetIdleTimer();

            if (habbo != null) {
                THashSet<Habbo> movedHabbos = new THashSet<>();
                movedHabbos.add(habbo);
                room.updateHabbosAt(targetTile.x, targetTile.y, movedHabbos);
            }

            roomUnit.setPreviousLocation(targetTile);
            roomUnit.setPreviousLocationZ(roomUnit.getZ());
            roomUnit.statusUpdate(false);
        });

        processTileCallbacks(room, roomUnit, oldLocation, targetTile, oldTopItem, newTopItem);
        room.sendComposer(new RoomUserStatusComposer(roomUnit).compose());
        return true;
    }

    private static void scheduleTileCallbacks(Room room, RoomUnit roomUnit, RoomTile oldLocation, RoomTile targetTile, HabboItem oldTopItem, HabboItem newTopItem, int delay) {
        if (oldTopItem == null && newTopItem == null) {
            return;
        }

        Emulator.getThreading().run(() -> {
            processTileCallbacks(room, roomUnit, oldLocation, targetTile, oldTopItem, newTopItem);
        }, Math.max(delay, InteractionRoller.DELAY));
    }

    private static void processTileCallbacks(Room room, RoomUnit roomUnit, RoomTile oldLocation, RoomTile targetTile, HabboItem oldTopItem, HabboItem newTopItem) {
        if (room == null || !room.isLoaded() || roomUnit == null || roomUnit.getCurrentLocation() == null) {
            return;
        }

        if (roomUnit.getCurrentLocation().x != targetTile.x || roomUnit.getCurrentLocation().y != targetTile.y) {
            return;
        }

        if (oldTopItem != null && oldTopItem != newTopItem) {
            try {
                oldTopItem.onWalkOff(roomUnit, room, new Object[]{oldLocation, targetTile});
            } catch (Exception exception) {
                LOGGER.error("Failed to process wired user walk off callback", exception);
            }
        }

        if (newTopItem != null && newTopItem != oldTopItem) {
            try {
                newTopItem.onWalkOn(roomUnit, room, new Object[]{oldLocation, targetTile});
            } catch (Exception exception) {
                LOGGER.error("Failed to process wired user walk on callback", exception);
            }
        }
    }

    private static void schedulePostureSync(Room room, RoomUnit roomUnit, RoomTile targetTile, int delay) {
        if (!roomUnit.hasStatus(RoomUnitStatus.SIT) && !roomUnit.hasStatus(RoomUnitStatus.LAY)) {
            return;
        }

        Emulator.getThreading().run(() -> {
            if (room == null || !room.isLoaded() || roomUnit == null || roomUnit.getCurrentLocation() == null) {
                return;
            }

            if (roomUnit.getCurrentLocation().x != targetTile.x || roomUnit.getCurrentLocation().y != targetTile.y) {
                return;
            }

            room.sendComposer(new RoomUserStatusComposer(roomUnit).compose());
        }, delay + STATUS_SUPPRESSION_GRACE_MS + 25);
    }

    private static void suppressStatusComposer(RoomUnit roomUnit, int duration) {
        if (roomUnit == null) {
            return;
        }

        long suppressedUntil = System.currentTimeMillis() + Math.max(duration, InteractionRoller.DELAY) + STATUS_SUPPRESSION_GRACE_MS;
        SUPPRESSED_STATUS_COMPOSER_UNTIL.put(roomUnit.getId(), suppressedUntil);
    }

    private static void runWithSuppressedStatusUpdates(Collection<RoomUnit> roomUnits, Runnable action) {
        if (action == null) {
            return;
        }

        Set<Integer> previousSuppressedRoomUnitIds = SUPPRESSED_STATUS_ROOM_UNIT_IDS.get();
        HashSet<Integer> suppressedRoomUnitIds = previousSuppressedRoomUnitIds == null
                ? new HashSet<>()
                : new HashSet<>(previousSuppressedRoomUnitIds);

        if (roomUnits != null) {
            for (RoomUnit roomUnit : roomUnits) {
                if (roomUnit != null) {
                    suppressedRoomUnitIds.add(roomUnit.getId());
                }
            }
        }

        if (suppressedRoomUnitIds.isEmpty()) {
            action.run();
            return;
        }

        SUPPRESSED_STATUS_ROOM_UNIT_IDS.set(suppressedRoomUnitIds);

        try {
            action.run();
        } finally {
            if (previousSuppressedRoomUnitIds == null || previousSuppressedRoomUnitIds.isEmpty()) {
                SUPPRESSED_STATUS_ROOM_UNIT_IDS.remove();
            } else {
                SUPPRESSED_STATUS_ROOM_UNIT_IDS.set(previousSuppressedRoomUnitIds);
            }
        }
    }
}

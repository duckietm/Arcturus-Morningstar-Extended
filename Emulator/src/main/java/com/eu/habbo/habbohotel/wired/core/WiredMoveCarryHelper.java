package com.eu.habbo.habbohotel.wired.core;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredExtra;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraAnimationTime;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraMoveCarryUsers;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraMovePhysics;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraMoveNoAnimation;
import com.eu.habbo.habbohotel.rooms.FurnitureMovementError;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.rooms.RoomUnitType;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.outgoing.rooms.WiredMovementsComposer;
import com.eu.habbo.messages.outgoing.rooms.items.FloorItemOnRollerComposer;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserStatusComposer;
import gnu.trove.set.hash.THashSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class WiredMoveCarryHelper {
    private static final double DIRECT_HEIGHT_TOLERANCE = 0.1D;
    private static final int STATUS_SUPPRESSION_GRACE_MS = 250;
    private static final ThreadLocal<Set<Integer>> SUPPRESSED_STATUS_ROOM_UNIT_IDS = new ThreadLocal<>();
    private static final ConcurrentHashMap<Integer, Long> SUPPRESSED_STATUS_COMPOSER_UNTIL = new ConcurrentHashMap<>();

    private WiredMoveCarryHelper() {
    }

    public static FurnitureMovementError getMovementError(Room room, HabboItem stackItem, HabboItem movingItem, RoomTile targetTile, int rotation, WiredContext ctx) {
        if (room == null || movingItem == null || targetTile == null) {
            return FurnitureMovementError.INVALID_MOVE;
        }

        if (!hasMovementBehaviorExtra(room, stackItem)) {
            return room.furnitureFitsAt(targetTile, movingItem, rotation, true);
        }

        CarryContext carryContext = prepareCarryContext(room, stackItem, movingItem, ctx);
        WiredMovementPhysics movementPhysics = getMovementPhysics(room, stackItem, movingItem, ctx);
        FurnitureMovementError movementError = room.furnitureFitsAtWithPhysics(targetTile, movingItem, rotation, false, movementPhysics);

        if (movementError != FurnitureMovementError.NONE) {
            return movementError;
        }

        if (!carryContext.active) {
            return room.furnitureFitsAtWithPhysics(targetTile, movingItem, rotation, true, movementPhysics);
        }

        return getBlockingUnitError(room, movingItem, targetTile, rotation, carryContext, movementPhysics);
    }

    public static FurnitureMovementError moveFurni(Room room, HabboItem stackItem, HabboItem movingItem, RoomTile targetTile, int rotation, Habbo actor, boolean sendUpdates, WiredContext ctx) {
        return moveFurni(room, stackItem, movingItem, targetTile, rotation, null, actor, sendUpdates, ctx);
    }

    public static FurnitureMovementError moveFurni(Room room, HabboItem stackItem, HabboItem movingItem, RoomTile targetTile, int rotation, Double z, Habbo actor, boolean sendUpdates, WiredContext ctx) {
        if (room == null || movingItem == null || targetTile == null) {
            return FurnitureMovementError.INVALID_MOVE;
        }

        if (!hasMovementBehaviorExtra(room, stackItem)) {
            return moveFurniLegacy(room, movingItem, targetTile, rotation, z, actor, sendUpdates);
        }

        RoomTile oldLocation = room.getLayout() == null ? null : room.getLayout().getTile(movingItem.getX(), movingItem.getY());
        double oldZ = movingItem.getZ();
        CarryContext carryContext = prepareCarryContext(room, stackItem, movingItem, ctx);
        WiredMovementPhysics movementPhysics = getMovementPhysics(room, stackItem, movingItem, ctx);
        FurnitureMovementError movementError = room.furnitureFitsAtWithPhysics(targetTile, movingItem, rotation, false, movementPhysics);

        if (movementError != FurnitureMovementError.NONE) {
            return movementError;
        }

        if (carryContext.active) {
            movementError = getBlockingUnitError(room, movingItem, targetTile, rotation, carryContext, movementPhysics);

            if (movementError != FurnitureMovementError.NONE) {
                return movementError;
            }
        } else {
            movementError = room.furnitureFitsAtWithPhysics(targetTile, movingItem, rotation, true, movementPhysics);

            if (movementError != FurnitureMovementError.NONE) {
                return movementError;
            }
        }

        boolean useWiredMovements = !hasNoAnimationExtra(room, stackItem);
        int animationDuration = getAnimationDuration(room, stackItem, WiredMovementsComposer.DEFAULT_DURATION);
        Set<Integer> previousSuppressedRoomUnitIds = SUPPRESSED_STATUS_ROOM_UNIT_IDS.get();

        if (carryContext.active) {
            HashSet<Integer> suppressedRoomUnitIds = previousSuppressedRoomUnitIds == null
                    ? new HashSet<>()
                    : new HashSet<>(previousSuppressedRoomUnitIds);
            suppressedRoomUnitIds.addAll(carryContext.carriedUserIds);
            SUPPRESSED_STATUS_ROOM_UNIT_IDS.set(suppressedRoomUnitIds);
        }

        FurnitureMovementError result;
        Double targetZ = z;

        if (targetZ == null && movementPhysics.isKeepAltitude()) {
            targetZ = oldZ;
        }

        try {
            result = (targetZ == null)
                    ? room.moveFurniToWithPhysics(movingItem, targetTile, rotation, actor, !useWiredMovements, false, movementPhysics)
                    : room.moveFurniToWithPhysics(movingItem, targetTile, rotation, targetZ, actor, !useWiredMovements, false, movementPhysics);
        } finally {
            if (carryContext.active) {
                if (previousSuppressedRoomUnitIds == null || previousSuppressedRoomUnitIds.isEmpty()) {
                    SUPPRESSED_STATUS_ROOM_UNIT_IDS.remove();
                } else {
                    SUPPRESSED_STATUS_ROOM_UNIT_IDS.set(previousSuppressedRoomUnitIds);
                }
            }
        }

        if (result == FurnitureMovementError.NONE) {
            if (!useWiredMovements) {
                applyInstantCarryState(room, movingItem, targetTile, rotation, carryContext);
            } else if (oldLocation != null) {
                sendAnimatedMove(room, movingItem, oldLocation, oldZ, targetTile, rotation, carryContext, animationDuration);
            }
        }

        return result;
    }

    private static FurnitureMovementError moveFurniLegacy(Room room, HabboItem movingItem, RoomTile targetTile, int rotation, Double z, Habbo actor, boolean sendUpdates) {
        if (room == null || movingItem == null || targetTile == null) {
            return FurnitureMovementError.INVALID_MOVE;
        }

        RoomTile oldLocation = room.getLayout() == null ? null : room.getLayout().getTile(movingItem.getX(), movingItem.getY());
        double oldZ = movingItem.getZ();

        FurnitureMovementError result = (z == null)
                ? room.moveFurniTo(movingItem, targetTile, rotation, actor, sendUpdates)
                : room.moveFurniTo(movingItem, targetTile, rotation, z, actor, sendUpdates, false);

        if (result == FurnitureMovementError.NONE
                && !sendUpdates
                && oldLocation != null
                && (oldLocation.x != targetTile.x || oldLocation.y != targetTile.y || Double.compare(oldZ, movingItem.getZ()) != 0)) {
            room.sendComposer(new FloorItemOnRollerComposer(movingItem, null, oldLocation, oldZ, targetTile, movingItem.getZ(), 0, room).compose());
        }

        return result;
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

    public static boolean hasNoAnimationExtra(Room room, HabboItem stackItem) {
        return getNoAnimationExtra(room, stackItem) != null;
    }

    public static int getAnimationDuration(Room room, HabboItem stackItem, int fallbackDuration) {
        WiredExtraAnimationTime extra = getAnimationTimeExtra(room, stackItem);
        return (extra != null) ? extra.getDurationMs() : fallbackDuration;
    }

    private static boolean hasMovementBehaviorExtra(Room room, HabboItem stackItem) {
        THashSet<InteractionWiredExtra> extras = getMovementExtras(room, stackItem);
        if (extras == null || extras.isEmpty()) {
            return false;
        }

        for (InteractionWiredExtra extra : extras) {
            if (extra instanceof WiredExtraMoveCarryUsers
                    || extra instanceof WiredExtraMoveNoAnimation
                    || extra instanceof WiredExtraAnimationTime
                    || extra instanceof WiredExtraMovePhysics) {
                return true;
            }
        }

        return false;
    }

    private static CarryContext prepareCarryContext(Room room, HabboItem stackItem, HabboItem movingItem, WiredContext ctx) {
        WiredExtraMoveCarryUsers extra = getActiveExtra(room, stackItem);

        if (extra == null || ctx == null || room.getLayout() == null) {
            return CarryContext.disabled();
        }

        RoomTile anchorTile = room.getLayout().getTile(movingItem.getX(), movingItem.getY());
        if (anchorTile == null) {
            return CarryContext.disabled();
        }

        THashSet<RoomTile> occupiedTiles = room.getLayout().getTilesAt(
                anchorTile,
                movingItem.getBaseItem().getWidth(),
                movingItem.getBaseItem().getLength(),
                movingItem.getRotation());

        if (occupiedTiles == null || occupiedTiles.isEmpty()) {
            return CarryContext.disabled();
        }

        Collection<RoomUnit> targetUsers = resolveUsers(room, ctx, extra.getUserSource());
        if (targetUsers == null || targetUsers.isEmpty()) {
            return CarryContext.disabled();
        }

        List<CarriedRoomUnit> carriedUnits = new ArrayList<>();
        HashSet<Integer> carriedIds = new HashSet<>();

        for (RoomUnit roomUnit : targetUsers) {
            if (!isEligibleUser(room, movingItem, roomUnit, occupiedTiles, extra.getCarryMode())) {
                continue;
            }

            CarriedRoomUnit carriedRoomUnit = new CarriedRoomUnit(
                    roomUnit,
                    roomUnit.getCurrentLocation(),
                    roomUnit.getZ(),
                    roomUnit.getZ() - getCarrySurfaceZ(movingItem, roomUnit, roomUnit.getZ()),
                    roomUnit.getX() - anchorTile.x,
                    roomUnit.getY() - anchorTile.y);

            carriedUnits.add(carriedRoomUnit);
            carriedIds.add(roomUnit.getId());
        }

        if (carriedUnits.isEmpty()) {
            return CarryContext.disabled();
        }

        return new CarryContext(true, carriedUnits, carriedIds);
    }

    private static Collection<RoomUnit> resolveUsers(Room room, WiredContext ctx, int userSource) {
        if (userSource == WiredExtraMoveCarryUsers.SOURCE_ALL_ROOM_USERS) {
            return new ArrayList<>(room.getRoomUnits());
        }

        return WiredSourceUtil.resolveUsers(ctx, userSource);
    }

    private static boolean isEligibleUser(Room room, HabboItem movingItem, RoomUnit roomUnit, THashSet<RoomTile> occupiedTiles, int carryMode) {
        if (roomUnit == null
                || roomUnit.getRoomUnitType() != RoomUnitType.USER
                || roomUnit.getCurrentLocation() == null
                || !roomUnit.isInRoom()
                || roomUnit.isWalking()) {
            return false;
        }

        Habbo habbo = room.getHabbo(roomUnit);
        if (habbo != null && habbo.getHabboInfo() != null && habbo.getHabboInfo().getRiding() != null) {
            return false;
        }

        if (!occupiesMovingFurni(occupiedTiles, roomUnit)) {
            return false;
        }

        if (carryMode == WiredExtraMoveCarryUsers.MODE_DIRECTLY_ON_FURNI) {
            return isDirectlyOnMovingFurni(room, movingItem, roomUnit);
        }

        return true;
    }

    private static boolean isDirectlyOnMovingFurni(Room room, HabboItem movingItem, RoomUnit roomUnit) {
        HabboItem topItem = room.getTopItemAt(roomUnit.getX(), roomUnit.getY());
        if (topItem == movingItem) {
            return true;
        }

        double carrySurfaceZ = getCarrySurfaceZ(movingItem, roomUnit, roomUnit.getZ());
        return Math.abs(roomUnit.getZ() - carrySurfaceZ) <= DIRECT_HEIGHT_TOLERANCE;
    }

    private static boolean occupiesMovingFurni(THashSet<RoomTile> occupiedTiles, RoomUnit roomUnit) {
        for (RoomTile occupiedTile : occupiedTiles) {
            if (occupiedTile != null
                    && occupiedTile.x == roomUnit.getX()
                    && occupiedTile.y == roomUnit.getY()) {
                return true;
            }
        }

        return false;
    }

    private static FurnitureMovementError getBlockingUnitError(Room room, HabboItem movingItem, RoomTile targetTile, int rotation, CarryContext carryContext, WiredMovementPhysics movementPhysics) {
        THashSet<RoomTile> occupiedTiles = room.getLayout().getTilesAt(
                targetTile,
                movingItem.getBaseItem().getWidth(),
                movingItem.getBaseItem().getLength(),
                rotation);

        if (occupiedTiles == null || occupiedTiles.isEmpty()) {
            return FurnitureMovementError.NONE;
        }

        for (RoomTile tile : occupiedTiles) {
            for (RoomUnit roomUnit : room.getRoomUnits(tile)) {
                if (roomUnit == null || carryContext.carriedUserIds.contains(roomUnit.getId())) {
                    continue;
                }

                if (movementPhysics.shouldIgnoreUser(roomUnit)) {
                    continue;
                }

                switch (roomUnit.getRoomUnitType()) {
                    case BOT:
                        return FurnitureMovementError.TILE_HAS_BOTS;
                    case PET:
                        return FurnitureMovementError.TILE_HAS_PETS;
                    case USER:
                    default:
                        return FurnitureMovementError.TILE_HAS_HABBOS;
                }
            }
        }

        return FurnitureMovementError.NONE;
    }

    private static void sendAnimatedMove(Room room, HabboItem movingItem, RoomTile oldLocation, double oldZ, RoomTile targetTile, int rotation, CarryContext carryContext, int animationDuration) {
        List<CarriedUnitMove> carriedMoves = getCarriedUnitMoves(room, movingItem, targetTile, rotation, carryContext);
        List<WiredMovementsComposer.MovementData> movements = new ArrayList<>();
        movements.add(WiredMovementsComposer.furniMovement(
                movingItem.getId(),
                oldLocation.x,
                oldLocation.y,
                targetTile.x,
                targetTile.y,
                oldZ,
                movingItem.getZ(),
                movingItem.getRotation(),
                animationDuration));

        for (CarriedUnitMove carriedMove : carriedMoves) {
            suppressStatusComposer(carriedMove.roomUnit, animationDuration);
            movements.add(WiredMovementsComposer.userSlideMovement(
                    carriedMove.roomUnit.getId(),
                    carriedMove.oldLocation.x,
                    carriedMove.oldLocation.y,
                    carriedMove.destinationTile.x,
                    carriedMove.destinationTile.y,
                    carriedMove.oldZ,
                    carriedMove.newZ,
                    carriedMove.roomUnit.getBodyRotation().getValue(),
                    carriedMove.roomUnit.getHeadRotation().getValue(),
                    animationDuration));
        }

        room.sendComposer(new WiredMovementsComposer(movements).compose());

        for (CarriedUnitMove carriedMove : carriedMoves) {
            updateCarriedUnitState(carriedMove);
        }
    }

    private static void applyInstantCarryState(Room room, HabboItem movingItem, RoomTile targetTile, int rotation, CarryContext carryContext) {
        if (!carryContext.active || room == null || movingItem == null || targetTile == null) {
            return;
        }

        List<CarriedUnitMove> carriedMoves = getCarriedUnitMoves(room, movingItem, targetTile, rotation, carryContext);

        for (CarriedUnitMove carriedMove : carriedMoves) {
            updateCarriedUnitStateInstant(carriedMove);

            Habbo habbo = room.getHabbo(carriedMove.roomUnit);
            if (habbo != null && shouldRefreshPostureWithTileUpdate(carriedMove.roomUnit)) {
                THashSet<Habbo> movedHabbos = new THashSet<>();
                movedHabbos.add(habbo);
                room.updateHabbosAt(carriedMove.destinationTile.x, carriedMove.destinationTile.y, movedHabbos);
            }

            room.sendComposer(new RoomUserStatusComposer(carriedMove.roomUnit).compose());
        }
    }

    private static List<CarriedUnitMove> getCarriedUnitMoves(Room room, HabboItem movingItem, RoomTile targetTile, int rotation, CarryContext carryContext) {
        List<CarriedUnitMove> carriedMoves = new ArrayList<>();

        if (!carryContext.active) {
            return carriedMoves;
        }

        THashSet<RoomTile> occupiedTiles = room.getLayout().getTilesAt(
                targetTile,
                movingItem.getBaseItem().getWidth(),
                movingItem.getBaseItem().getLength(),
                rotation);

        for (CarriedRoomUnit carriedRoomUnit : carryContext.carriedUsers) {
            RoomUnit roomUnit = carriedRoomUnit.roomUnit;

            if (roomUnit == null || roomUnit.getCurrentLocation() == null || roomUnit.isWalking()) {
                continue;
            }

            RoomTile destinationTile = room.getLayout().getTile(
                    (short) (targetTile.x + carriedRoomUnit.relativeX),
                    (short) (targetTile.y + carriedRoomUnit.relativeY));

            if (destinationTile == null || destinationTile.state == null || !occupiedTiles.contains(destinationTile)) {
                destinationTile = targetTile;
            }

            double carrySurfaceZ = getCarrySurfaceZ(movingItem, roomUnit, carriedRoomUnit.oldZ);
            double newZ = carrySurfaceZ + carriedRoomUnit.heightOffset;
            carriedMoves.add(new CarriedUnitMove(roomUnit, carriedRoomUnit.oldLocation, carriedRoomUnit.oldZ, destinationTile, newZ));
        }

        return carriedMoves;
    }

    private static boolean shouldRefreshPostureWithTileUpdate(RoomUnit roomUnit) {
        return roomUnit != null
                && (roomUnit.hasStatus(RoomUnitStatus.SIT) || roomUnit.hasStatus(RoomUnitStatus.LAY));
    }

    private static double getCarrySurfaceZ(HabboItem movingItem, RoomUnit roomUnit, double referenceZ) {
        if (movingItem == null) {
            return referenceZ;
        }

        double baseZ = movingItem.getZ();
        double topZ = baseZ + Item.getCurrentHeight(movingItem);

        if (roomUnit != null && (roomUnit.hasStatus(RoomUnitStatus.SIT) || roomUnit.hasStatus(RoomUnitStatus.LAY))) {
            return baseZ;
        }

        if (movingItem.getBaseItem().allowSit() || movingItem.getBaseItem().allowLay()) {
            return (Math.abs(referenceZ - baseZ) <= Math.abs(referenceZ - topZ)) ? baseZ : topZ;
        }

        return topZ;
    }

    private static void updateCarriedUnitState(CarriedUnitMove carriedMove) {
        carriedMove.roomUnit.setLocation(carriedMove.destinationTile);
        carriedMove.roomUnit.setZ(carriedMove.newZ);
        carriedMove.roomUnit.setLastRollerTime(System.currentTimeMillis());
        carriedMove.roomUnit.setPreviousLocation(carriedMove.destinationTile);
        carriedMove.roomUnit.setPreviousLocationZ(carriedMove.newZ);

        if (carriedMove.roomUnit.hasStatus(RoomUnitStatus.SIT)) {
            carriedMove.roomUnit.sitUpdate = true;
        }
    }

    private static void updateCarriedUnitStateInstant(CarriedUnitMove carriedMove) {
        carriedMove.roomUnit.setLocation(carriedMove.destinationTile);
        carriedMove.roomUnit.setZ(carriedMove.newZ);
        carriedMove.roomUnit.setPreviousLocation(carriedMove.destinationTile);
        carriedMove.roomUnit.setPreviousLocationZ(carriedMove.newZ);
        carriedMove.roomUnit.statusUpdate(false);

        if (carriedMove.roomUnit.hasStatus(RoomUnitStatus.SIT)) {
            carriedMove.roomUnit.sitUpdate = true;
        }
    }

    private static void suppressStatusComposer(RoomUnit roomUnit, int duration) {
        if (roomUnit == null) {
            return;
        }

        long suppressedUntil = System.currentTimeMillis() + Math.max(duration, WiredMovementsComposer.DEFAULT_DURATION) + STATUS_SUPPRESSION_GRACE_MS;
        SUPPRESSED_STATUS_COMPOSER_UNTIL.put(roomUnit.getId(), suppressedUntil);
    }

    private static WiredExtraMoveCarryUsers getActiveExtra(Room room, HabboItem stackItem) {
        THashSet<InteractionWiredExtra> extras = getMovementExtras(room, stackItem);
        if (extras == null || extras.isEmpty()) {
            return null;
        }

        for (InteractionWiredExtra extra : extras) {
            if (extra instanceof WiredExtraMoveCarryUsers) {
                return (WiredExtraMoveCarryUsers) extra;
            }
        }

        return null;
    }

    private static WiredExtraMoveNoAnimation getNoAnimationExtra(Room room, HabboItem stackItem) {
        THashSet<InteractionWiredExtra> extras = getMovementExtras(room, stackItem);
        if (extras == null || extras.isEmpty()) {
            return null;
        }

        for (InteractionWiredExtra extra : extras) {
            if (extra instanceof WiredExtraMoveNoAnimation) {
                return (WiredExtraMoveNoAnimation) extra;
            }
        }

        return null;
    }

    private static WiredExtraAnimationTime getAnimationTimeExtra(Room room, HabboItem stackItem) {
        THashSet<InteractionWiredExtra> extras = getMovementExtras(room, stackItem);
        if (extras == null || extras.isEmpty()) {
            return null;
        }

        for (InteractionWiredExtra extra : extras) {
            if (extra instanceof WiredExtraAnimationTime) {
                return (WiredExtraAnimationTime) extra;
            }
        }

        return null;
    }

    private static WiredMovementPhysics getMovementPhysics(Room room, HabboItem stackItem, HabboItem movingItem, WiredContext ctx) {
        WiredExtraMovePhysics extra = getMovementPhysicsExtra(room, stackItem);
        if (extra == null) {
            return WiredMovementPhysics.NONE;
        }

        HashSet<Integer> passThroughFurniIds = new HashSet<>();
        HashSet<Integer> passThroughUserIds = new HashSet<>();
        HashSet<Integer> blockingFurniIds = new HashSet<>();

        if (extra.isMoveThroughFurni()) {
            for (HabboItem item : resolveFurniSources(room, ctx, extra.getMoveThroughFurniSource())) {
                if (item != null && item != movingItem) {
                    passThroughFurniIds.add(item.getId());
                }
            }
        }

        if (extra.isMoveThroughUsers()) {
            for (RoomUnit roomUnit : resolvePhysicsUsers(room, ctx, extra.getMoveThroughUsersSource())) {
                if (roomUnit != null && roomUnit.getRoomUnitType() == RoomUnitType.USER) {
                    passThroughUserIds.add(roomUnit.getId());
                }
            }
        }

        if (extra.isBlockByFurni()) {
            for (HabboItem item : resolveFurniSources(room, ctx, extra.getBlockByFurniSource())) {
                if (item != null && item != movingItem) {
                    blockingFurniIds.add(item.getId());
                }
            }
        }

        return new WiredMovementPhysics(extra.isKeepAltitude(), passThroughFurniIds, passThroughUserIds, blockingFurniIds);
    }

    private static Collection<HabboItem> resolveFurniSources(Room room, WiredContext ctx, int sourceType) {
        if (room == null) {
            return new ArrayList<>();
        }

        if (sourceType == WiredExtraMovePhysics.SOURCE_ALL_ROOM) {
            return new ArrayList<>(room.getFloorItems());
        }

        if (ctx == null) {
            return new ArrayList<>();
        }

        return WiredSourceUtil.resolveItems(ctx, sourceType, null);
    }

    private static Collection<RoomUnit> resolvePhysicsUsers(Room room, WiredContext ctx, int userSource) {
        if (room == null) {
            return new ArrayList<>();
        }

        if (userSource == WiredExtraMovePhysics.SOURCE_ALL_ROOM) {
            return new ArrayList<>(room.getRoomUnits());
        }

        if (ctx == null) {
            return new ArrayList<>();
        }

        return WiredSourceUtil.resolveUsers(ctx, userSource);
    }

    private static WiredExtraMovePhysics getMovementPhysicsExtra(Room room, HabboItem stackItem) {
        THashSet<InteractionWiredExtra> extras = getMovementExtras(room, stackItem);
        if (extras == null || extras.isEmpty()) {
            return null;
        }

        for (InteractionWiredExtra extra : extras) {
            if (extra instanceof WiredExtraMovePhysics) {
                return (WiredExtraMovePhysics) extra;
            }
        }

        return null;
    }

    private static THashSet<InteractionWiredExtra> getMovementExtras(Room room, HabboItem stackItem) {
        if (room == null || stackItem == null || room.getRoomSpecialTypes() == null) {
            return null;
        }

        THashSet<InteractionWiredExtra> extras = room.getRoomSpecialTypes().getExtras(stackItem.getX(), stackItem.getY());
        if (extras == null || extras.isEmpty()) {
            return null;
        }

        return extras;
    }

    private static final class CarryContext {
        private final boolean active;
        private final List<CarriedRoomUnit> carriedUsers;
        private final HashSet<Integer> carriedUserIds;

        private CarryContext(boolean active, List<CarriedRoomUnit> carriedUsers, HashSet<Integer> carriedUserIds) {
            this.active = active;
            this.carriedUsers = carriedUsers;
            this.carriedUserIds = carriedUserIds;
        }

        private static CarryContext disabled() {
            return new CarryContext(false, new ArrayList<>(), new HashSet<>());
        }
    }

    private static final class CarriedRoomUnit {
        private final RoomUnit roomUnit;
        private final RoomTile oldLocation;
        private final double oldZ;
        private final double heightOffset;
        private final int relativeX;
        private final int relativeY;

        private CarriedRoomUnit(RoomUnit roomUnit, RoomTile oldLocation, double oldZ, double heightOffset, int relativeX, int relativeY) {
            this.roomUnit = roomUnit;
            this.oldLocation = oldLocation;
            this.oldZ = oldZ;
            this.heightOffset = heightOffset;
            this.relativeX = relativeX;
            this.relativeY = relativeY;
        }
    }

    private static final class CarriedUnitMove {
        private final RoomUnit roomUnit;
        private final RoomTile oldLocation;
        private final double oldZ;
        private final RoomTile destinationTile;
        private final double newZ;

        private CarriedUnitMove(RoomUnit roomUnit, RoomTile oldLocation, double oldZ, RoomTile destinationTile, double newZ) {
            this.roomUnit = roomUnit;
            this.oldLocation = oldLocation;
            this.oldZ = oldZ;
            this.destinationTile = destinationTile;
            this.newZ = newZ;
        }
    }
}

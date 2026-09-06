package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.FurnitureType;
import com.eu.habbo.habbohotel.items.interactions.InteractionDefault;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomTileState;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.outgoing.rooms.items.AddFloorItemComposer;
import com.eu.habbo.messages.outgoing.rooms.items.FloorItemOnRollerComposer;
import com.eu.habbo.messages.outgoing.rooms.items.RemoveFloorItemComposer;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUnitOnRollerComposer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class TrashCommand extends Command {
    private static final int RAIN_CLOUD_EFFECT = 113;
    private static final int FLYING_EFFECT = 116;
    private static final int PIRATE_CREW_EFFECT = 161;
    private static final int SHARK_COUNT = 7;
    private static final int ANIMATION_TICKS = 38;
    private static final long ANIMATION_TICK_MS = 280L;
    private static final int TORNADO_TICKS = 34;
    private static final long TORNADO_TICK_MS = 260L;
    private static final int MAX_TORNADO_FURNI = 40;
    private static final AtomicInteger TRANSIENT_ITEM_IDS = new AtomicInteger(1_900_000_000);
    private static final List<String> SHARK_ITEM_NAMES = List.of(
            "js_r16_shark", "seven_hammerheadshark", "seven_shark", "bw_fin", "bw_jaws");

    public TrashCommand() {
        super("cmd_trash", Emulator.getTexts().getValue("commands.keys.cmd_trash").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        Habbo caller = gameClient.getHabbo();
        Room room = caller.getHabboInfo().getCurrentRoom();
        if (room == null) return true;
        if (!RoomFunCommandAccess.requireOwnerOrStaff(caller, room)) return true;

        String commandKey = params.length == 0 ? "sharknado" : params[0].replace(":", "");
        if (commandKey.equalsIgnoreCase("tornado") || commandKey.equalsIgnoreCase("trash")) {
            return handleTornado(gameClient, params.length >= 2 ? params[1] : null);
        }
        return handleSharknado(gameClient, params.length >= 2 ? params[1] : null);
    }

    private static boolean handleSharknado(GameClient gameClient, String targetName) {
        Habbo caller = gameClient.getHabbo();
        Room room = caller.getHabboInfo().getCurrentRoom();
        if (room == null) return true;
        Habbo target = targetName == null ? caller : room.getHabbo(targetName);
        if (target == null) {
            caller.whisper(
                    Emulator.getTexts().getValue("commands.error.target_not_found").replace("%user%", targetName),
                    RoomChatMessageBubbles.ALERT);
            return true;
        }

        Long eventId = RoomFunEventLock.tryAcquire(room);
        if (eventId == null) {
            caller.whisper(
                    Emulator.getTexts().getValue("commands.error.cmd_trash.already_active"),
                    RoomChatMessageBubbles.ALERT);
            return true;
        }

        List<Item> sharkBaseItems = resolveSharkBaseItems();
        List<RoomTile> orbit = buildOrbit(room, target.getRoomUnit().getCurrentLocation());
        if (sharkBaseItems.isEmpty() || orbit.size() < 2) {
            RoomFunEventLock.release(room, eventId);
            caller.whisper(
                    Emulator.getTexts().getValue("commands.error.cmd_trash.unavailable"),
                    RoomChatMessageBubbles.ALERT);
            return true;
        }

        List<PreviousEffect> previousEffects = captureEffects(target);
        List<TransientShark> sharks = spawnSharks(room, caller, sharkBaseItems, orbit);
        for (PreviousEffect previous : previousEffects) {
            room.giveEffect(previous.habbo(), RAIN_CLOUD_EFFECT, 3);
            previous.habbo().whisper(
                    Emulator.getTexts().getValue("commands.action.cmd_trash.warning"),
                    RoomChatMessageBubbles.THUNDER);
        }

        Emulator.getThreading().run(
                () -> applyAvatarStage(room, eventId, previousEffects, FLYING_EFFECT, 6), 2_800L);
        Emulator.getThreading().run(
                () -> applyAvatarStage(room, eventId, previousEffects, PIRATE_CREW_EFFECT, 4), 8_200L);
        Emulator.getThreading().run(
                () -> animateSharks(room, eventId, sharks, orbit, previousEffects, 1), ANIMATION_TICK_MS);
        return true;
    }

    private static boolean handleTornado(GameClient gameClient, String targetName) {
        Habbo caller = gameClient.getHabbo();
        Room room = caller.getHabboInfo().getCurrentRoom();
        if (room == null) return true;
        Habbo target = targetName == null ? caller : room.getHabbo(targetName);
        if (target == null) {
            caller.whisper(
                    Emulator.getTexts().getValue("commands.error.target_not_found").replace("%user%", targetName),
                    RoomChatMessageBubbles.ALERT);
            return true;
        }

        Long eventId = RoomFunEventLock.tryAcquire(room);
        if (eventId == null) {
            caller.whisper(
                    Emulator.getTexts().getValue("commands.error.cmd_trash.already_active"),
                    RoomChatMessageBubbles.ALERT);
            return true;
        }

        RoomUnit unit = target.getRoomUnit();
        List<RoomTile> orbit = buildOrbit(room, unit.getCurrentLocation());
        if (orbit.size() < 2) {
            RoomFunEventLock.release(room, eventId);
            caller.whisper(
                    Emulator.getTexts().getValue("commands.error.cmd_tornado.unavailable"),
                    RoomChatMessageBubbles.ALERT);
            return true;
        }

        TornadoUserState userState = new TornadoUserState(
                target,
                unit.getCurrentLocation(),
                unit.getZ(),
                unit.canWalk(),
                unit.getEffectId(),
                unit.getEffectEndTimestamp());
        List<TornadoFurni> furni = captureTornadoFurni(room, orbit.size());
        unit.setCanWalk(false);
        room.giveEffect(target, RAIN_CLOUD_EFFECT, 3);
        target.whisper(
                Emulator.getTexts().getValue("commands.action.cmd_tornado.warning"),
                RoomChatMessageBubbles.THUNDER);
        Emulator.getThreading().run(
                () -> animateTornado(room, eventId, orbit, furni, userState, 1), TORNADO_TICK_MS);
        return true;
    }

    private static List<TornadoFurni> captureTornadoFurni(Room room, int orbitSize) {
        List<HabboItem> roomFurni = new ArrayList<>(room.getFloorItems());
        roomFurni.sort(Comparator.comparingInt(HabboItem::getId));
        int count = Math.min(MAX_TORNADO_FURNI, roomFurni.size());
        List<TornadoFurni> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            HabboItem item = roomFurni.get((i * roomFurni.size()) / Math.max(1, count));
            RoomTile originalTile = room.getLayout().getTile(item.getX(), item.getY());
            if (originalTile == null) continue;
            result.add(new TornadoFurni(item, originalTile, item.getZ(), (i * orbitSize) / Math.max(1, count), i));
        }
        return result;
    }

    private static void animateTornado(
            Room room,
            long eventId,
            List<RoomTile> orbit,
            List<TornadoFurni> furni,
            TornadoUserState userState,
            int tick) {
        if (!isActive(room, eventId)) {
            restoreTornado(room, furni, userState);
            return;
        }

        for (TornadoFurni captured : furni) {
            RoomTile oldTile = tick == 1
                    ? captured.originalTile()
                    : orbit.get(Math.floorMod(tick - 2 + captured.orbitOffset(), orbit.size()));
            RoomTile newTile = orbit.get(Math.floorMod(tick - 1 + captured.orbitOffset(), orbit.size()));
            double oldZ = tick == 1
                    ? captured.originalZ()
                    : tornadoHeight(oldTile, tick - 1, captured.layer());
            double newZ = tornadoHeight(newTile, tick, captured.layer());
            room.sendComposer(new FloorItemOnRollerComposer(
                            captured.item(), null, oldTile, oldZ, newTile, newZ, newZ - oldZ, room)
                    .compose());
        }

        Habbo caller = userState.habbo();
        if (caller.getHabboInfo().getCurrentRoom() == room) {
            RoomTile oldTile = tick == 1
                    ? userState.originalTile()
                    : orbit.get(Math.floorMod(tick - 2, orbit.size()));
            RoomTile newTile = orbit.get(Math.floorMod(tick - 1, orbit.size()));
            double oldZ = tick == 1 ? userState.originalZ() : tornadoUserHeight(oldTile, tick - 1);
            double newZ = tornadoUserHeight(newTile, tick);
            room.sendComposer(new RoomUnitOnRollerComposer(
                            caller.getRoomUnit(), null, oldTile, oldZ, newTile, newZ, room)
                    .compose());
        }

        if (tick < TORNADO_TICKS) {
            Emulator.getThreading().run(
                    () -> animateTornado(room, eventId, orbit, furni, userState, tick + 1),
                    TORNADO_TICK_MS);
        } else {
            try {
                restoreTornado(room, furni, userState);
            } finally {
                RoomFunEventLock.release(room, eventId);
            }
        }
    }

    private static void restoreTornado(
            Room room, List<TornadoFurni> furni, TornadoUserState userState) {
        for (TornadoFurni captured : furni) {
            if (captured.item().getRoomId() == room.getId()) {
                room.sendComposer(new RemoveFloorItemComposer(captured.item(), true).compose());
                room.sendComposer(new AddFloorItemComposer(
                                captured.item(),
                                room.getFurniOwnerNames().get(captured.item().getUserId()))
                        .compose());
            } else {
                room.sendComposer(new RemoveFloorItemComposer(captured.item(), true).compose());
            }
        }

        Habbo caller = userState.habbo();
        if (caller.getHabboInfo().getCurrentRoom() != room) return;

        RoomUnit unit = caller.getRoomUnit();
        RoomTile currentTile = unit.getCurrentLocation();
        room.sendComposer(new RoomUnitOnRollerComposer(
                        unit,
                        null,
                        currentTile,
                        unit.getZ(),
                        userState.originalTile(),
                        userState.originalZ(),
                        room)
                .compose());
        unit.setGoalLocation(userState.originalTile());
        unit.setCanWalk(userState.couldWalk());
        int now = Emulator.getIntUnixTimestamp();
        int duration = userState.effectEndTimestamp() == Integer.MAX_VALUE
                ? -1
                : Math.max(1, userState.effectEndTimestamp() - now);
        room.giveEffect(unit, userState.effectId(), userState.effectId() == 0 ? -1 : duration);
        caller.whisper(
                Emulator.getTexts().getValue("commands.action.cmd_tornado.finished"),
                RoomChatMessageBubbles.THUNDER);
    }

    private static double tornadoHeight(RoomTile tile, int tick, int layer) {
        double wave = Math.abs(Math.sin((tick * 0.5) + (layer * 0.7)));
        return tile.z + 0.75 + (wave * 5.0) + ((layer % 4) * 0.35);
    }

    private static double tornadoUserHeight(RoomTile tile, int tick) {
        return tile.z + 1.5 + (Math.abs(Math.sin(tick * 0.45)) * 4.5);
    }

    private static List<Item> resolveSharkBaseItems() {
        List<Item> result = new ArrayList<>();
        for (String itemName : SHARK_ITEM_NAMES) {
            Item item = Emulator.getGameEnvironment().getItemManager().getItem(itemName);
            // Floor only: the sharks orbit on the floor, and the client resolves a floor object's
            // type id against the floor furnidata. A wall furni here (bw_jaws lives in
            // wallitemtypes) is never found there, so it draws as the placeholder cube.
            if (item != null && item.getType() == FurnitureType.FLOOR) result.add(item);
        }
        return result;
    }

    private static List<PreviousEffect> captureEffects(Room room) {
        List<PreviousEffect> previousEffects = new ArrayList<>();
        for (Habbo habbo : room.getHabbos()) {
            RoomUnit unit = habbo.getRoomUnit();
            previousEffects.add(new PreviousEffect(habbo, unit.getEffectId(), unit.getEffectEndTimestamp()));
        }
        return previousEffects;
    }

    private static List<PreviousEffect> captureEffects(Habbo habbo) {
        RoomUnit unit = habbo.getRoomUnit();
        return List.of(new PreviousEffect(habbo, unit.getEffectId(), unit.getEffectEndTimestamp()));
    }

    private static List<TransientShark> spawnSharks(
            Room room, Habbo caller, List<Item> sharkBaseItems, List<RoomTile> orbit) {
        List<TransientShark> sharks = new ArrayList<>();
        int count = Math.min(SHARK_COUNT, Math.max(3, orbit.size()));
        for (int i = 0; i < count; i++) {
            int orbitOffset = (i * orbit.size()) / count;
            RoomTile start = orbit.get(orbitOffset);
            Item baseItem = sharkBaseItems.get(i % sharkBaseItems.size());
            HabboItem shark = new InteractionDefault(
                    TRANSIENT_ITEM_IDS.getAndIncrement(),
                    caller.getHabboInfo().getId(),
                    baseItem,
                    "0",
                    0,
                    0);
            shark.setRoomId(room.getId());
            shark.setX(start.x);
            shark.setY(start.y);
            shark.setZ(flightHeight(start, 0, i));
            shark.setRotation((i * 2) % 8);
            shark.needsUpdate(false);
            room.sendComposer(new AddFloorItemComposer(shark, "Sharknado").compose());
            sharks.add(new TransientShark(shark, orbitOffset, i));
        }
        return sharks;
    }

    private static void animateSharks(
            Room room,
            long eventId,
            List<TransientShark> sharks,
            List<RoomTile> orbit,
            List<PreviousEffect> previousEffects,
            int tick) {
        if (!isActive(room, eventId)) {
            removeSharks(room, sharks);
            return;
        }

        for (TransientShark shark : sharks) {
            int oldIndex = Math.floorMod(tick - 1 + shark.orbitOffset(), orbit.size());
            int newIndex = Math.floorMod(tick + shark.orbitOffset(), orbit.size());
            RoomTile oldTile = orbit.get(oldIndex);
            RoomTile newTile = orbit.get(newIndex);
            double oldZ = flightHeight(oldTile, tick - 1, shark.layer());
            double newZ = flightHeight(newTile, tick, shark.layer());
            room.sendComposer(new FloorItemOnRollerComposer(
                            shark.item(), null, oldTile, oldZ, newTile, newZ, newZ - oldZ, room)
                    .compose());
        }

        if (tick < ANIMATION_TICKS) {
            Emulator.getThreading().run(
                    () -> animateSharks(room, eventId, sharks, orbit, previousEffects, tick + 1),
                    ANIMATION_TICK_MS);
        } else {
            finishEvent(room, eventId, sharks, previousEffects);
        }
    }

    private static void applyAvatarStage(
            Room room, long eventId, List<PreviousEffect> previousEffects, int effectId, int duration) {
        if (!isActive(room, eventId)) return;

        for (PreviousEffect previous : previousEffects) {
            Habbo habbo = previous.habbo();
            if (habbo.getHabboInfo().getCurrentRoom() == room) {
                room.giveEffect(habbo, effectId, duration);
            }
        }
    }

    private static void finishEvent(
            Room room, long eventId, List<TransientShark> sharks, List<PreviousEffect> previousEffects) {
        if (!isActive(room, eventId)) return;

        try {
            removeSharks(room, sharks);
            restoreEffects(room, previousEffects);
        } finally {
            RoomFunEventLock.release(room, eventId);
        }
    }

    private static void removeSharks(Room room, List<TransientShark> sharks) {
        for (TransientShark shark : sharks) {
            room.sendComposer(new RemoveFloorItemComposer(shark.item(), true).compose());
        }
    }

    private static void restoreEffects(Room room, List<PreviousEffect> previousEffects) {
        int now = Emulator.getIntUnixTimestamp();
        for (PreviousEffect previous : previousEffects) {
            Habbo habbo = previous.habbo();
            if (habbo.getHabboInfo().getCurrentRoom() != room
                    || habbo.getRoomUnit().getEffectId() != PIRATE_CREW_EFFECT) {
                continue;
            }

            int duration = previous.endTimestamp() == Integer.MAX_VALUE
                    ? -1
                    : Math.max(1, previous.endTimestamp() - now);
            room.giveEffect(habbo, previous.effectId(), previous.effectId() == 0 ? -1 : duration);
            habbo.whisper(
                    Emulator.getTexts().getValue("commands.action.cmd_trash.finished"),
                    RoomChatMessageBubbles.PIRATE);
        }
    }

    private static List<RoomTile> buildOrbit(Room room, RoomTile center) {
        List<RoomTile> usable = new ArrayList<>();
        for (short x = 0; x < room.getLayout().getMapSizeX(); x++) {
            for (short y = 0; y < room.getLayout().getMapSizeY(); y++) {
                RoomTile tile = room.getLayout().getTile(x, y);
                if (tile != null && tile.state != RoomTileState.INVALID) usable.add(tile);
            }
        }
        if (usable.size() < 2) return usable;

        double radiusX = Math.max(1.0, Math.min(5.0, room.getLayout().getMapSizeX() / 3.0));
        double radiusY = Math.max(1.0, Math.min(5.0, room.getLayout().getMapSizeY() / 3.0));
        List<RoomTile> orbit = new ArrayList<>();
        Set<Integer> used = new HashSet<>();
        for (int step = 0; step < 24; step++) {
            double angle = (Math.PI * 2.0 * step) / 24.0;
            double targetX = center.x + (Math.cos(angle) * radiusX);
            double targetY = center.y + (Math.sin(angle) * radiusY);
            RoomTile nearest = usable.stream()
                    .min(Comparator.comparingDouble(tile ->
                            Math.pow(tile.x - targetX, 2) + Math.pow(tile.y - targetY, 2)))
                    .orElse(null);
            if (nearest != null && used.add((nearest.x << 16) ^ (nearest.y & 0xFFFF))) orbit.add(nearest);
        }

        if (orbit.size() < 4) {
            usable.sort(Comparator.comparingDouble(
                    tile -> Math.atan2(tile.y - center.y, tile.x - center.x)));
            orbit.clear();
            orbit.addAll(usable.subList(0, Math.min(usable.size(), 16)));
        }
        return orbit;
    }

    private static double flightHeight(RoomTile tile, int tick, int layer) {
        return tile.z + 1.25 + (Math.abs(Math.sin((tick * 0.42) + (layer * 1.15))) * 3.75);
    }

    private static boolean isActive(Room room, long eventId) {
        return RoomFunEventLock.isActive(room, eventId);
    }

    private record PreviousEffect(Habbo habbo, int effectId, int endTimestamp) {}

    private record TransientShark(HabboItem item, int orbitOffset, int layer) {}

    private record TornadoFurni(
            HabboItem item, RoomTile originalTile, double originalZ, int orbitOffset, int layer) {}

    private record TornadoUserState(
            Habbo habbo,
            RoomTile originalTile,
            double originalZ,
            boolean couldWalk,
            int effectId,
            int effectEndTimestamp) {}
}

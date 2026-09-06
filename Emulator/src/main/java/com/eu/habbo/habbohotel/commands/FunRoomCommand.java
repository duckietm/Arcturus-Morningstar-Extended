package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionBackgroundToner;
import com.eu.habbo.habbohotel.items.interactions.InteractionDefault;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessage;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomTileState;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.DanceType;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.outgoing.rooms.items.AddFloorItemComposer;
import com.eu.habbo.messages.outgoing.rooms.items.FloorItemOnRollerComposer;
import com.eu.habbo.messages.outgoing.rooms.items.FloorItemUpdateComposer;
import com.eu.habbo.messages.outgoing.rooms.items.RemoveFloorItemComposer;
import com.eu.habbo.messages.outgoing.rooms.RoomPaintComposer;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUnitOnRollerComposer;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserTalkComposer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class FunRoomCommand extends Command {
    private static final int MAX_EARTHQUAKE_FURNI = 50;
    private static final AtomicInteger TRANSIENT_ITEM_IDS = new AtomicInteger(1_910_000_000);
    private static final Map<String, RoomShow> ROOM_SHOWS = Map.ofEntries(
            Map.entry("disco", new RoomShow(new int[] {189, 190, 137, 4}, true, Motion.NONE, true, false)),
            Map.entry("rave", new RoomShow(new int[] {190, 4, 217, 218}, true, Motion.NONE, true, false)),
            Map.entry("levitazione", new RoomShow(new int[] {116, 4}, false, Motion.BOUNCE, false, false)),
            Map.entry("girotondo", new RoomShow(new int[] {4, 7, 8}, true, Motion.ORBIT, false, false)),
            Map.entry("invasionefufo", new RoomShow(new int[] {3, 17, 18, 297}, false, Motion.BOUNCE, false, false)),
            Map.entry("piratiparty", new RoomShow(new int[] {160, 161, 162, 164}, true, Motion.NONE, false, false)),
            Map.entry("ghostparty", new RoomShow(new int[] {13, 92, 132}, true, Motion.NONE, false, false)),
            Map.entry("robotparty", new RoomShow(new int[] {187, 188, 199}, true, Motion.NONE, false, false)),
            Map.entry("pioggiadisco", new RoomShow(new int[] {113, 189, 190}, true, Motion.NONE, true, false)),
            Map.entry("caos", new RoomShow(new int[] {79, 108, 165, 166, 169, 170}, true, Motion.NONE, false, false)),
            Map.entry("statue", new RoomShow(new int[] {12}, false, Motion.NONE, false, true)),
            Map.entry("carnevale", new RoomShow(new int[] {141, 142, 143, 144, 145, 146}, true, Motion.NONE, false, false)));
    private static final Map<String, TargetShow> TARGET_SHOWS = Map.ofEntries(
            Map.entry("rapisci", new TargetShow(3, Motion.LIFT)),
            Map.entry("rimbalza", new TargetShow(193, Motion.BOUNCE)),
            Map.entry("vola", new TargetShow(116, Motion.LIFT)),
            Map.entry("orbita", new TargetShow(4, Motion.ORBIT)),
            Map.entry("frullatore", new TargetShow(165, Motion.ORBIT)),
            Map.entry("yoyo", new TargetShow(116, Motion.BOUNCE)),
            Map.entry("fantasma", new TargetShow(13, Motion.NONE)),
            Map.entry("papera", new TargetShow(170, Motion.NONE)),
            Map.entry("mummia", new TargetShow(84, Motion.NONE)),
            Map.entry("zombie", new TargetShow(88, Motion.NONE)),
            Map.entry("goblin", new TargetShow(166, Motion.NONE)),
            Map.entry("alieno", new TargetShow(79, Motion.NONE)));
    private static final Map<String, PairInteraction> PAIR_INTERACTIONS = Map.ofEntries(
            Map.entry("scambio", PairInteraction.SWAP),
            Map.entry("calamita", PairInteraction.MAGNET),
            Map.entry("duello", PairInteraction.DUEL),
            Map.entry("abbraccio", PairInteraction.HUG),
            Map.entry("telepatia", PairInteraction.TELEPATHY),
            Map.entry("catapulta", PairInteraction.CATAPULT));
    private static final Map<String, Atmosphere> ATMOSPHERES = Map.ofEntries(
            Map.entry("spazio", new Atmosphere(
                    new int[][] {{170, 255, 28}, {185, 255, 42}, {205, 230, 30}, {225, 255, 45}},
                    new String[] {"7.12", "1.11", "7.7"}, 4, 116)),
            Map.entry("aurora", new Atmosphere(
                    new int[][] {{95, 220, 42}, {125, 255, 48}, {155, 240, 40}, {190, 230, 45}},
                    new String[] {"6.7", "7.7", "5.7"}, 8, 4)),
            Map.entry("temporale", new Atmosphere(
                    new int[][] {{150, 80, 22}, {155, 110, 35}, {0, 0, 100}, {160, 90, 18}},
                    new String[] {"1.9", "7.12", "1.9"}, 3, 113)),
            Map.entry("tramonto", new Atmosphere(
                    new int[][] {{5, 245, 48}, {15, 255, 58}, {28, 235, 52}, {240, 180, 32}},
                    new String[] {"3.3", "4.3", "5.3"}, 5, 141)),
            Map.entry("neonvoid", new Atmosphere(
                    new int[][] {{215, 255, 40}, {190, 255, 52}, {235, 255, 45}, {145, 255, 42}},
                    new String[] {"1.6", "4.6", "7.6"}, 2, 190)),
            Map.entry("blackout", new Atmosphere(
                    new int[][] {{0, 0, 4}, {0, 0, 2}, {0, 0, 55}, {0, 0, 1}},
                    new String[] {"1.10", "1.10", "1.10"}, 1, 12)),
            Map.entry("oceano", new Atmosphere(
                    new int[][] {{135, 230, 36}, {145, 255, 44}, {160, 210, 32}, {125, 245, 48}},
                    new String[] {"2.4", "3.4", "4.4"}, 6, 108)),
            Map.entry("inferno", new Atmosphere(
                    new int[][] {{0, 255, 38}, {8, 255, 50}, {18, 255, 44}, {250, 240, 32}},
                    new String[] {"1.5", "3.5", "7.5"}, 7, 165)));

    public FunRoomCommand() {
        super("cmd_trash", Emulator.getTexts().getValue("commands.keys.cmd_fun_room").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        String key = params.length == 0 ? "fun" : params[0].replace(":", "").toLowerCase();
        if (key.equals("fun")) {
            gameClient.getHabbo().alert(Emulator.getTexts().getValue("commands.help.cmd_fun_room"));
            return true;
        }

        Room room = gameClient.getHabbo().getHabboInfo().getCurrentRoom();
        if (room == null) return true;
        if (!RoomFunCommandAccess.requireOwnerOrStaff(gameClient.getHabbo(), room)) return true;
        if (key.equals("terremoto")) return startEarthquake(gameClient, room);
        if (key.equals("cannoni")) return startCannonBarrage(gameClient, room);

        Atmosphere atmosphere = ATMOSPHERES.get(key);
        if (atmosphere != null) return startAtmosphere(gameClient, room, atmosphere, key);

        PairInteraction interaction = PAIR_INTERACTIONS.get(key);
        if (interaction != null) return startPairInteraction(gameClient, room, interaction, key, params);

        RoomShow roomShow = ROOM_SHOWS.get(key);
        if (roomShow != null) return startRoomShow(gameClient, room, roomShow, key);

        TargetShow targetShow = TARGET_SHOWS.get(key);
        if (targetShow != null) return startTargetShow(gameClient, room, targetShow, key, params);
        return true;
    }

    private static boolean startCannonBarrage(GameClient gameClient, Room room) {
        Long eventId = acquire(gameClient, room);
        if (eventId == null) return true;

        List<CannonLane> lanes = buildCannonLanes(room);
        if (lanes.isEmpty()) {
            RoomFunEventLock.release(room, eventId);
            gameClient.getHabbo().whisper(
                    Emulator.getTexts().getValue("commands.error.cmd_cannoni.no_space"),
                    RoomChatMessageBubbles.ALERT);
            return true;
        }

        List<HabboItem> cannons = spawnCannons(room, gameClient.getHabbo(), lanes);
        if (cannons.isEmpty()) {
            RoomFunEventLock.release(room, eventId);
            gameClient.getHabbo().whisper(
                    Emulator.getTexts().getValue("commands.error.cmd_cannoni.no_asset"),
                    RoomChatMessageBubbles.ALERT);
            return true;
        }

        broadcast(room, "commands.action.cmd_cannoni.started", "%event%", "cannoni");
        List<CannonProjectile> projectiles = new ArrayList<>();
        Emulator.getThreading().run(
                () -> animateCannonBarrage(room, eventId, gameClient.getHabbo(), lanes, cannons, projectiles, 1),
                220L);
        return true;
    }

    private static void animateCannonBarrage(
            Room room,
            long eventId,
            Habbo owner,
            List<CannonLane> lanes,
            List<HabboItem> cannons,
            List<CannonProjectile> projectiles,
            int tick) {
        if (!RoomFunEventLock.isActive(room, eventId)) {
            finishCannonBarrage(room, eventId, cannons, projectiles);
            return;
        }

        try {
            for (HabboItem cannon : cannons) {
                cannon.setExtradata("0");
                room.sendComposer(new FloorItemUpdateComposer(cannon).compose());
            }

            if (tick <= 34 && tick % 2 == 1) {
                int shots = (tick % 6 == 1) ? 2 : 1;
                for (int i = 0; i < shots; i++) {
                    int laneIndex = Math.floorMod((tick * 7) + (i * 5), lanes.size());
                    CannonLane lane = lanes.get(laneIndex);
                    HabboItem cannon = cannons.get(laneIndex % cannons.size());
                    cannon.setExtradata("1");
                    room.sendComposer(new FloorItemUpdateComposer(cannon).compose());
                    CannonProjectile projectile = spawnProjectile(room, owner, lane, tick + i);
                    if (projectile != null) projectiles.add(projectile);
                }
            }

            for (CannonProjectile projectile : new ArrayList<>(projectiles)) {
                if (projectile.exploded) {
                    removeProjectile(room, projectile, projectiles);
                    continue;
                }

                updateHomingDirection(room, projectile);
                RoomTile next = room.getLayout().getTile(
                        (short) (projectile.tile.x + projectile.dx),
                        (short) (projectile.tile.y + projectile.dy));
                if (next == null || next.state == RoomTileState.INVALID) {
                    removeProjectile(room, projectile, projectiles);
                    continue;
                }

                Set<HabboItem> blockers = room.getItemsAt(next);
                if (!blockers.isEmpty()) {
                    impactProjectile(room, projectile, blockers.iterator().next());
                    continue;
                }

                if (!room.getHabbosAt(next).isEmpty()) {
                    for (Habbo hit : room.getHabbosAt(next)) {
                        UserState hitState = captureUser(hit);
                        room.giveEffect(hit, 165, 2);
                        room.dance(hit, DanceType.values()[1 + Math.floorMod(tick, 4)]);
                        sendVisualMove(room, hitState, next, next.z + 3.0);
                        Emulator.getThreading().run(
                                () -> restoreUser(room, hitState, true),
                                1_900L);
                    }
                    impactProjectile(room, projectile, null);
                    continue;
                }

                double nextZ = next.z + 0.65 + Math.abs(Math.sin((tick + projectile.phase) * 0.9)) * 1.4;
                room.sendComposer(new FloorItemOnRollerComposer(
                                projectile.item,
                                null,
                                projectile.tile,
                                projectile.z,
                                next,
                                nextZ,
                                nextZ - projectile.z,
                                room)
                        .compose());
                projectile.tile = next;
                projectile.z = nextZ;
            }

            if (tick < 50 || !projectiles.isEmpty()) {
                Emulator.getThreading().run(
                        () -> animateCannonBarrage(
                                room, eventId, owner, lanes, cannons, projectiles, tick + 1),
                        220L);
            } else {
                finishCannonBarrage(room, eventId, cannons, projectiles);
            }
        } catch (RuntimeException exception) {
            finishCannonBarrage(room, eventId, cannons, projectiles);
            throw exception;
        }
    }

    private static List<CannonLane> buildCannonLanes(Room room) {
        List<CannonLane> lanes = new ArrayList<>();
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (short x = 0; x < room.getLayout().getMapSizeX(); x++) {
            for (short y = 0; y < room.getLayout().getMapSizeY(); y++) {
                RoomTile tile = room.getLayout().getTile(x, y);
                if (tile == null || tile.state == RoomTileState.INVALID) continue;
                for (int[] direction : directions) {
                    RoomTile outward = room.getLayout().getTile(
                            (short) (x - direction[0]), (short) (y - direction[1]));
                    RoomTile inward = room.getLayout().getTile(
                            (short) (x + direction[0]), (short) (y + direction[1]));
                    if ((outward == null || outward.state == RoomTileState.INVALID)
                            && inward != null
                            && inward.state != RoomTileState.INVALID) {
                        lanes.add(new CannonLane(tile, direction[0], direction[1], cannonRotation(direction)));
                    }
                }
            }
        }
        Collections.shuffle(lanes);
        return new ArrayList<>(lanes.subList(0, Math.min(8, lanes.size())));
    }

    private static int cannonRotation(int[] direction) {
        if (direction[0] > 0) return 2;
        if (direction[0] < 0) return 6;
        if (direction[1] > 0) return 4;
        return 0;
    }

    private static List<HabboItem> spawnCannons(Room room, Habbo owner, List<CannonLane> lanes) {
        Item cannonBase = firstItem("pirate_cannon", "cannon", "scifi_c17_xcannon");
        if (cannonBase == null) return List.of();
        List<HabboItem> cannons = new ArrayList<>();
        for (CannonLane lane : lanes) {
            HabboItem cannon = transientItem(owner, cannonBase, lane.start, lane.start.z + 0.05, "0", lane.rotation);
            room.sendComposer(new AddFloorItemComposer(cannon, "Cannoni").compose());
            cannons.add(cannon);
        }
        return cannons;
    }

    private static CannonProjectile spawnProjectile(
            Room room, Habbo owner, CannonLane lane, int phase) {
        String[][] choices = {
            {"fball_ball", "wf_glowball", "fball_ball2"},
            {"wf_glowball", "fball_ball3", "fball_ball"},
            {"fball_ball2", "fball_ball4", "wf_glowball"}
        };
        List<Habbo> possibleTargets = new ArrayList<>(room.getHabbos());
        Habbo homingTarget = phase % 3 == 0 && !possibleTargets.isEmpty()
                ? possibleTargets.get(Math.floorMod(phase * 5, possibleTargets.size()))
                : null;
        Item projectileBase = homingTarget != null
                ? firstItem("wf_glowball", "fball_ball3", "fball_ball")
                : firstItem(choices[Math.floorMod(phase, choices.length)]);
        if (projectileBase == null) return null;
        double z = lane.start.z + 1.0;
        HabboItem item = transientItem(owner, projectileBase, lane.start, z, "0", 0);
        room.sendComposer(new AddFloorItemComposer(item, "Cannoni").compose());
        return new CannonProjectile(item, lane.start, lane.dx, lane.dy, z, phase, homingTarget);
    }

    private static void updateHomingDirection(Room room, CannonProjectile projectile) {
        Habbo target = projectile.homingTarget;
        if (target == null || target.getHabboInfo().getCurrentRoom() != room) return;

        RoomTile targetTile = target.getRoomUnit().getCurrentLocation();
        int deltaX = targetTile.x - projectile.tile.x;
        int deltaY = targetTile.y - projectile.tile.y;
        if (deltaX == 0 && deltaY == 0) return;

        int preferredDx = 0;
        int preferredDy = 0;
        int alternateDx = 0;
        int alternateDy = 0;
        if (Math.abs(deltaX) >= Math.abs(deltaY)) {
            preferredDx = Integer.signum(deltaX);
            alternateDy = Integer.signum(deltaY);
        } else {
            preferredDy = Integer.signum(deltaY);
            alternateDx = Integer.signum(deltaX);
        }

        if (isValidProjectileStep(room, projectile.tile, preferredDx, preferredDy)) {
            projectile.dx = preferredDx;
            projectile.dy = preferredDy;
        } else if (isValidProjectileStep(room, projectile.tile, alternateDx, alternateDy)) {
            projectile.dx = alternateDx;
            projectile.dy = alternateDy;
        }
    }

    private static boolean isValidProjectileStep(
            Room room, RoomTile origin, int dx, int dy) {
        if (dx == 0 && dy == 0) return false;
        RoomTile tile = room.getLayout().getTile(
                (short) (origin.x + dx), (short) (origin.y + dy));
        return tile != null && tile.state != RoomTileState.INVALID;
    }

    private static HabboItem transientItem(
            Habbo owner, Item base, RoomTile tile, double z, String extraData, int rotation) {
        HabboItem item = new InteractionDefault(
                TRANSIENT_ITEM_IDS.getAndIncrement(), owner.getHabboInfo().getId(), base, extraData, 0, 0);
        item.setRoomId(owner.getHabboInfo().getCurrentRoom().getId());
        item.setX(tile.x);
        item.setY(tile.y);
        item.setZ(z);
        item.setRotation(rotation);
        item.needsUpdate(false);
        return item;
    }

    private static Item firstItem(String... names) {
        for (String name : names) {
            Item item = Emulator.getGameEnvironment().getItemManager().getItem(name);
            if (item != null) return item;
        }
        return null;
    }

    private static void impactProjectile(Room room, CannonProjectile projectile, HabboItem blocker) {
        projectile.exploded = true;
        projectile.item.setExtradata("1");
        room.sendComposer(new FloorItemUpdateComposer(projectile.item).compose());
        if (blocker != null && blocker.getRoomId() == room.getId()) {
            RoomTile tile = room.getLayout().getTile(blocker.getX(), blocker.getY());
            if (tile != null) {
                room.sendComposer(new FloorItemOnRollerComposer(
                                blocker,
                                null,
                                tile,
                                blocker.getZ(),
                                tile,
                                blocker.getZ() + 1.1,
                                1.1,
                                room)
                        .compose());
                Emulator.getThreading().run(
                        () -> {
                            if (blocker.getRoomId() == room.getId()) {
                                room.sendComposer(new FloorItemUpdateComposer(blocker).compose());
                            }
                        },
                        320L);
            }
        }
    }

    private static void removeProjectile(
            Room room, CannonProjectile projectile, List<CannonProjectile> projectiles) {
        room.sendComposer(new RemoveFloorItemComposer(projectile.item, true).compose());
        projectiles.remove(projectile);
    }

    private static void finishCannonBarrage(
            Room room,
            long eventId,
            List<HabboItem> cannons,
            List<CannonProjectile> projectiles) {
        try {
            for (CannonProjectile projectile : new ArrayList<>(projectiles)) {
                room.sendComposer(new RemoveFloorItemComposer(projectile.item, true).compose());
            }
            projectiles.clear();
            for (HabboItem cannon : cannons) {
                room.sendComposer(new RemoveFloorItemComposer(cannon, true).compose());
            }
            broadcast(room, "commands.action.cmd_cannoni.finished", "%event%", "cannoni");
        } finally {
            RoomFunEventLock.release(room, eventId);
        }
    }

    private static boolean startAtmosphere(
            GameClient gameClient, Room room, Atmosphere atmosphere, String commandKey) {
        Long eventId = acquire(gameClient, room);
        if (eventId == null) return true;

        HabboItem toner = spawnAtmosphereToner(room, gameClient.getHabbo(), atmosphere.palette()[0]);
        String originalLandscape = room.getBackgroundPaint();
        List<UserState> users = captureUsers(room);
        broadcast(room, "commands.action.cmd_fun_room.started", "%event%", commandKey);
        Emulator.getThreading().run(
                () -> animateAtmosphere(
                        room, eventId, atmosphere, toner, originalLandscape, users, commandKey, 1),
                280L);
        return true;
    }

    private static void animateAtmosphere(
            Room room,
            long eventId,
            Atmosphere atmosphere,
            HabboItem toner,
            String originalLandscape,
            List<UserState> users,
            String commandKey,
            int tick) {
        if (!RoomFunEventLock.isActive(room, eventId)) {
            restoreAtmosphere(room, eventId, toner, originalLandscape, users);
            return;
        }

        int[] color = atmosphere.palette()[(tick - 1) % atmosphere.palette().length];
        if (toner != null) {
            toner.setExtradata("1:" + color[0] + ":" + color[1] + ":" + color[2]);
            room.sendComposer(new FloorItemUpdateComposer(toner).compose());
        }
        if (tick == 1 || tick % 8 == 0) {
            int landscapeIndex = Math.floorMod((tick - 1) / 8, atmosphere.landscapes().length);
            room.sendComposer(new RoomPaintComposer(
                            "landscape", atmosphere.landscapes()[landscapeIndex])
                    .compose());
        }
        if (tick == 1 || tick % 5 == 0) {
            int index = 0;
            for (Habbo habbo : room.getHabbos()) {
                room.giveEffect(habbo, (index++ % 3 == 0) ? atmosphere.accentEffect() : atmosphere.baseEffect(), 2);
            }
        }

        if (tick < 36) {
            Emulator.getThreading().run(
                    () -> animateAtmosphere(
                            room, eventId, atmosphere, toner, originalLandscape, users, commandKey, tick + 1),
                    280L);
        } else {
            restoreAtmosphere(room, eventId, toner, originalLandscape, users);
        }
    }

    private static HabboItem spawnAtmosphereToner(Room room, Habbo owner, int[] color) {
        Item baseItem = Emulator.getGameEnvironment().getItemManager().getItem("roombg_color");
        if (baseItem == null) return null;
        RoomTile tile = owner.getRoomUnit().getCurrentLocation();
        HabboItem toner = new InteractionBackgroundToner(
                TRANSIENT_ITEM_IDS.getAndIncrement(),
                owner.getHabboInfo().getId(),
                baseItem,
                "1:" + color[0] + ":" + color[1] + ":" + color[2],
                0,
                0);
        toner.setRoomId(room.getId());
        toner.setX(tile.x);
        toner.setY(tile.y);
        toner.setZ(-20.0);
        toner.setRotation(0);
        toner.needsUpdate(false);
        room.sendComposer(new AddFloorItemComposer(toner, "Atmosfera").compose());
        return toner;
    }

    private static void restoreAtmosphere(
            Room room,
            long eventId,
            HabboItem toner,
            String originalLandscape,
            List<UserState> users) {
        try {
            if (toner != null) room.sendComposer(new RemoveFloorItemComposer(toner, true).compose());
            room.sendComposer(new RoomPaintComposer("landscape", originalLandscape).compose());
            for (HabboItem item : room.getFloorItems()) {
                if (item instanceof InteractionBackgroundToner) {
                    room.sendComposer(new FloorItemUpdateComposer(item).compose());
                }
            }
            for (UserState user : users) restoreUser(room, user, false);
            broadcast(room, "commands.action.cmd_fun_room.finished", "%event%", "atmosphere");
        } finally {
            RoomFunEventLock.release(room, eventId);
        }
    }

    private static boolean startPairInteraction(
            GameClient gameClient,
            Room room,
            PairInteraction interaction,
            String commandKey,
            String[] params) {
        if (params.length != 2) {
            gameClient.getHabbo().whisper(
                    Emulator.getTexts().getValue("commands.error.cmd_fun_room.usage")
                            .replace("%command%", commandKey),
                    RoomChatMessageBubbles.ALERT);
            return true;
        }

        Habbo actor = gameClient.getHabbo();
        Habbo target = room.getHabbo(params[1]);
        if (target == null) {
            actor.whisper(
                    Emulator.getTexts().getValue("commands.error.target_not_found").replace("%user%", params[1]),
                    RoomChatMessageBubbles.ALERT);
            return true;
        }
        if (target == actor) {
            actor.whisper(Emulator.getTexts().getValue("commands.error.target_self"), RoomChatMessageBubbles.ALERT);
            return true;
        }

        Long eventId = acquire(gameClient, room);
        if (eventId == null) return true;

        UserState actorState = captureUser(actor);
        UserState targetState = captureUser(target);
        actor.getRoomUnit().setCanWalk(false);
        target.getRoomUnit().setCanWalk(false);
        int[] effects = interaction.effects();
        room.giveEffect(actor, effects[0], 8);
        room.giveEffect(target, effects[1], 8);

        String interactionText = Emulator.getTexts()
                .getValue("commands.action.cmd_fun_pair." + commandKey)
                .replace("%actor%", actor.getHabboInfo().getUsername())
                .replace("%target%", target.getHabboInfo().getUsername());
        room.sendComposer(new RoomUserTalkComposer(new RoomChatMessage(
                        interactionText, actor, actor, interaction.bubble()))
                .compose());

        RoomTile center = midpointTile(room, actorState.originalTile(), targetState.originalTile());
        List<RoomTile> orbit = buildOrbit(room, center);
        Emulator.getThreading().run(
                () -> animatePairInteraction(
                        room, eventId, interaction, actorState, targetState, center, orbit, 1),
                240L);
        return true;
    }

    private static void animatePairInteraction(
            Room room,
            long eventId,
            PairInteraction interaction,
            UserState actor,
            UserState target,
            RoomTile center,
            List<RoomTile> orbit,
            int tick) {
        if (!RoomFunEventLock.isActive(room, eventId)
                || actor.habbo().getHabboInfo().getCurrentRoom() != room
                || target.habbo().getHabboInfo().getCurrentRoom() != room) {
            finishPairInteraction(room, eventId, actor, target);
            return;
        }

        switch (interaction) {
            case SWAP -> animateSwap(room, actor, target, tick);
            case MAGNET, HUG -> animateMagnet(room, actor, target, center, tick, interaction == PairInteraction.HUG);
            case DUEL -> animateDuel(room, actor, target, tick);
            case TELEPATHY -> animateTelepathy(room, actor, target, orbit, tick);
            case CATAPULT -> animateCatapult(room, actor, target, orbit, tick);
        }

        if (tick < interaction.ticks()) {
            Emulator.getThreading().run(
                    () -> animatePairInteraction(
                            room, eventId, interaction, actor, target, center, orbit, tick + 1),
                    240L);
        } else {
            finishPairInteraction(room, eventId, actor, target);
        }
    }

    private static void animateSwap(Room room, UserState actor, UserState target, int tick) {
        boolean crossed = (tick % 8) >= 4;
        RoomTile actorTile = crossed ? target.originalTile() : actor.originalTile();
        RoomTile targetTile = crossed ? actor.originalTile() : target.originalTile();
        double arc = 0.5 + Math.sin((tick % 4) * Math.PI / 4.0) * 2.5;
        sendVisualMove(room, actor, actorTile, actorTile.z + arc);
        sendVisualMove(room, target, targetTile, targetTile.z + arc);
    }

    private static void animateMagnet(
            Room room, UserState actor, UserState target, RoomTile center, int tick, boolean hug) {
        boolean together = (tick % 10) >= 3 && (tick % 10) <= 7;
        RoomTile actorTile = together ? center : actor.originalTile();
        RoomTile targetTile = together ? center : target.originalTile();
        double lift = hug ? 0.65 : 0.5 + Math.abs(Math.sin(tick * 0.8)) * 1.4;
        sendVisualMove(room, actor, actorTile, actorTile.z + lift);
        sendVisualMove(room, target, targetTile, targetTile.z + lift);
        if (hug && tick % 5 == 0) {
            room.giveEffect(actor.habbo(), 9, 2);
            room.giveEffect(target.habbo(), 9, 2);
        }
    }

    private static void animateDuel(Room room, UserState actor, UserState target, int tick) {
        double actorLift = tick % 2 == 0 ? 3.5 : 0.5;
        double targetLift = tick % 2 == 0 ? 0.5 : 3.5;
        sendVisualMove(room, actor, actor.originalTile(), actor.originalTile().z + actorLift);
        sendVisualMove(room, target, target.originalTile(), target.originalTile().z + targetLift);
        if (tick % 4 == 0) {
            room.giveEffect(actor.habbo(), tick % 8 == 0 ? 33 : 34, 2);
            room.giveEffect(target.habbo(), tick % 8 == 0 ? 34 : 33, 2);
        }
    }

    private static void animateTelepathy(
            Room room, UserState actor, UserState target, List<RoomTile> orbit, int tick) {
        if (orbit.size() < 2) {
            animateDuel(room, actor, target, tick);
            return;
        }
        RoomTile actorTile = orbit.get(Math.floorMod(tick - 1, orbit.size()));
        RoomTile targetTile = orbit.get(Math.floorMod(tick - 1 + orbit.size() / 2, orbit.size()));
        sendVisualMove(room, actor, actorTile, actorTile.z + 1.2);
        sendVisualMove(room, target, targetTile, targetTile.z + 1.2);
    }

    private static void animateCatapult(
            Room room, UserState actor, UserState target, List<RoomTile> orbit, int tick) {
        sendVisualMove(
                room,
                actor,
                actor.originalTile(),
                actor.originalTile().z + 0.5 + Math.abs(Math.sin(tick * 0.7)));
        RoomTile targetTile = orbit.isEmpty()
                ? target.originalTile()
                : orbit.get(Math.floorMod(tick - 1, orbit.size()));
        double flight = targetTile.z + 1.0 + Math.sin(Math.PI * tick / 24.0) * 8.0;
        sendVisualMove(room, target, targetTile, flight);
    }

    private static void sendVisualMove(Room room, UserState state, RoomTile tile, double z) {
        RoomUnit unit = state.habbo().getRoomUnit();
        room.sendComposer(new RoomUnitOnRollerComposer(
                        unit, null, unit.getCurrentLocation(), unit.getZ(), tile, z, room)
                .compose());
    }

    private static void finishPairInteraction(
            Room room, long eventId, UserState actor, UserState target) {
        try {
            restoreUser(room, actor, true);
            restoreUser(room, target, true);
        } finally {
            RoomFunEventLock.release(room, eventId);
        }
    }

    private static boolean startRoomShow(
            GameClient gameClient, Room room, RoomShow show, String commandKey) {
        Long eventId = acquire(gameClient, room);
        if (eventId == null) return true;

        List<UserState> users = captureUsers(room);
        if (show.motion() != Motion.NONE || show.freeze()) {
            users.forEach(state -> state.habbo().getRoomUnit().setCanWalk(false));
        }
        HabboItem discoBall = show.discoBall() ? spawnDiscoBall(room, gameClient.getHabbo()) : null;
        broadcast(room, "commands.action.cmd_fun_room.started", "%event%", commandKey);

        long interval = show.motion() == Motion.NONE ? 850L : 260L;
        int ticks = show.motion() == Motion.NONE ? 12 : 34;
        List<RoomTile> orbit = buildOrbit(room, gameClient.getHabbo().getRoomUnit().getCurrentLocation());
        Emulator.getThreading().run(
                () -> animateRoomShow(room, eventId, show, users, discoBall, orbit, 1, ticks, interval), interval);
        return true;
    }

    private static void animateRoomShow(
            Room room,
            long eventId,
            RoomShow show,
            List<UserState> users,
            HabboItem discoBall,
            List<RoomTile> orbit,
            int tick,
            int totalTicks,
            long interval) {
        if (!RoomFunEventLock.isActive(room, eventId)) {
            restoreRoomShow(room, eventId, show, users, discoBall);
            return;
        }

        int effect = show.effects()[(tick - 1) % show.effects().length];
        for (int i = 0; i < users.size(); i++) {
            UserState state = users.get(i);
            Habbo habbo = state.habbo();
            if (habbo.getHabboInfo().getCurrentRoom() != room) continue;
            if (tick == 1 || tick % 4 == 0) room.giveEffect(habbo, effect, 2);
            if (show.dance() && (tick == 1 || tick % 3 == 0)) {
                room.dance(habbo, DanceType.values()[1 + Math.floorMod(tick + i, 4)]);
            }
            animateUser(room, state, show.motion(), orbit, tick, i);
        }

        if (discoBall != null) {
            discoBall.setExtradata(Integer.toString(tick % Math.max(1, discoBall.getBaseItem().getStateCount())));
            room.sendComposer(new FloorItemUpdateComposer(discoBall).compose());
        }

        if (tick < totalTicks) {
            Emulator.getThreading().run(
                    () -> animateRoomShow(
                            room, eventId, show, users, discoBall, orbit, tick + 1, totalTicks, interval),
                    interval);
        } else {
            restoreRoomShow(room, eventId, show, users, discoBall);
        }
    }

    private static void restoreRoomShow(
            Room room, long eventId, RoomShow show, List<UserState> users, HabboItem discoBall) {
        try {
            if (discoBall != null) room.sendComposer(new RemoveFloorItemComposer(discoBall, true).compose());
            for (UserState state : users) restoreUser(room, state, show.motion() != Motion.NONE);
            broadcast(room, "commands.action.cmd_fun_room.finished", "%event%", "show");
        } finally {
            RoomFunEventLock.release(room, eventId);
        }
    }

    private static boolean startTargetShow(
            GameClient gameClient, Room room, TargetShow show, String commandKey, String[] params) {
        if (params.length != 2) {
            gameClient.getHabbo().whisper(
                    Emulator.getTexts().getValue("commands.error.cmd_fun_room.usage")
                            .replace("%command%", commandKey),
                    RoomChatMessageBubbles.ALERT);
            return true;
        }
        Habbo target = room.getHabbo(params[1]);
        if (target == null) {
            gameClient.getHabbo().whisper(
                    Emulator.getTexts().getValue("commands.error.target_not_found").replace("%user%", params[1]),
                    RoomChatMessageBubbles.ALERT);
            return true;
        }

        Long eventId = acquire(gameClient, room);
        if (eventId == null) return true;
        UserState state = captureUser(target);
        room.giveEffect(target, show.effect(), 8);
        target.whisper(
                Emulator.getTexts().getValue("commands.action.cmd_fun_target.started")
                        .replace("%event%", commandKey),
                RoomChatMessageBubbles.THUNDER);

        if (show.motion() == Motion.NONE) {
            Emulator.getThreading().run(() -> finishTargetShow(room, eventId, state), 8_000L);
            return true;
        }

        target.getRoomUnit().setCanWalk(false);
        List<RoomTile> orbit = buildOrbit(room, state.originalTile());
        Emulator.getThreading().run(
                () -> animateTargetShow(room, eventId, state, show.motion(), orbit, 1), 240L);
        return true;
    }

    private static void animateTargetShow(
            Room room, long eventId, UserState state, Motion motion, List<RoomTile> orbit, int tick) {
        if (!RoomFunEventLock.isActive(room, eventId)
                || state.habbo().getHabboInfo().getCurrentRoom() != room) {
            finishTargetShow(room, eventId, state);
            return;
        }

        animateUser(room, state, motion, orbit, tick, 0);
        if (tick < 30) {
            Emulator.getThreading().run(
                    () -> animateTargetShow(room, eventId, state, motion, orbit, tick + 1), 240L);
        } else {
            finishTargetShow(room, eventId, state);
        }
    }

    private static void finishTargetShow(Room room, long eventId, UserState state) {
        try {
            restoreUser(room, state, true);
        } finally {
            RoomFunEventLock.release(room, eventId);
        }
    }

    private static boolean startEarthquake(GameClient gameClient, Room room) {
        Long eventId = acquire(gameClient, room);
        if (eventId == null) return true;
        List<QuakeItem> items = captureQuakeItems(room);
        if (items.isEmpty()) {
            RoomFunEventLock.release(room, eventId);
            gameClient.getHabbo().whisper(
                    Emulator.getTexts().getValue("commands.error.cmd_terremoto.no_furni"),
                    RoomChatMessageBubbles.ALERT);
            return true;
        }
        broadcast(room, "commands.action.cmd_terremoto.started", "%event%", "terremoto");
        Emulator.getThreading().run(() -> animateEarthquake(room, eventId, items, 1), 180L);
        return true;
    }

    private static void animateEarthquake(Room room, long eventId, List<QuakeItem> items, int tick) {
        if (!RoomFunEventLock.isActive(room, eventId)) {
            restoreEarthquake(room, eventId, items);
            return;
        }
        boolean outward = (tick % 2) == 1;
        for (QuakeItem captured : items) {
            RoomTile oldTile = outward ? captured.originalTile() : captured.shakeTile();
            RoomTile newTile = outward ? captured.shakeTile() : captured.originalTile();
            double oldZ = captured.originalZ() + (outward ? 0.0 : 0.55);
            double newZ = captured.originalZ() + (outward ? 0.55 : 0.0);
            room.sendComposer(new FloorItemOnRollerComposer(
                            captured.item(), null, oldTile, oldZ, newTile, newZ, newZ - oldZ, room)
                    .compose());
        }
        if (tick < 24) {
            Emulator.getThreading().run(() -> animateEarthquake(room, eventId, items, tick + 1), 180L);
        } else {
            restoreEarthquake(room, eventId, items);
        }
    }

    private static void restoreEarthquake(Room room, long eventId, List<QuakeItem> items) {
        try {
            for (QuakeItem item : items) {
                if (item.item().getRoomId() == room.getId()) {
                    room.sendComposer(new FloorItemUpdateComposer(item.item()).compose());
                }
            }
        } finally {
            RoomFunEventLock.release(room, eventId);
        }
    }

    private static void animateUser(
            Room room, UserState state, Motion motion, List<RoomTile> orbit, int tick, int offset) {
        if (motion == Motion.NONE || state.habbo().getHabboInfo().getCurrentRoom() != room) return;
        RoomUnit unit = state.habbo().getRoomUnit();
        RoomTile oldTile;
        RoomTile newTile;
        if (motion == Motion.ORBIT && orbit.size() >= 2) {
            oldTile = tick == 1
                    ? state.originalTile()
                    : orbit.get(Math.floorMod(tick - 2 + offset * 2, orbit.size()));
            newTile = orbit.get(Math.floorMod(tick - 1 + offset * 2, orbit.size()));
        } else {
            oldTile = state.originalTile();
            newTile = state.originalTile();
        }
        double oldZ = tick == 1 ? state.originalZ() : motionHeight(oldTile, motion, tick - 1, offset);
        double newZ = motionHeight(newTile, motion, tick, offset);
        room.sendComposer(new RoomUnitOnRollerComposer(unit, null, oldTile, oldZ, newTile, newZ, room).compose());
    }

    private static double motionHeight(RoomTile tile, Motion motion, int tick, int offset) {
        double speed = motion == Motion.BOUNCE ? 0.9 : motion == Motion.LIFT ? 0.22 : 0.45;
        double amplitude = motion == Motion.LIFT ? 7.0 : motion == Motion.BOUNCE ? 3.5 : 2.0;
        return tile.z + 0.5 + Math.abs(Math.sin((tick * speed) + offset)) * amplitude;
    }

    private static List<UserState> captureUsers(Room room) {
        List<UserState> users = new ArrayList<>();
        for (Habbo habbo : room.getHabbos()) users.add(captureUser(habbo));
        return users;
    }

    private static UserState captureUser(Habbo habbo) {
        RoomUnit unit = habbo.getRoomUnit();
        return new UserState(
                habbo,
                unit.getCurrentLocation(),
                unit.getZ(),
                unit.canWalk(),
                unit.getEffectId(),
                unit.getEffectEndTimestamp(),
                unit.getDanceType());
    }

    private static void restoreUser(Room room, UserState state, boolean restorePosition) {
        Habbo habbo = state.habbo();
        if (habbo.getHabboInfo().getCurrentRoom() != room) return;
        RoomUnit unit = habbo.getRoomUnit();
        if (restorePosition) {
            room.sendComposer(new RoomUnitOnRollerComposer(
                            unit,
                            null,
                            unit.getCurrentLocation(),
                            unit.getZ(),
                            state.originalTile(),
                            state.originalZ(),
                            room)
                    .compose());
            unit.setGoalLocation(state.originalTile());
        }
        unit.setCanWalk(state.couldWalk());
        int now = Emulator.getIntUnixTimestamp();
        int duration = state.effectEndTimestamp() == Integer.MAX_VALUE
                ? -1
                : Math.max(1, state.effectEndTimestamp() - now);
        room.giveEffect(unit, state.effectId(), state.effectId() == 0 ? -1 : duration);
        room.dance(unit, state.danceType());
    }

    private static HabboItem spawnDiscoBall(Room room, Habbo owner) {
        Item baseItem = Emulator.getGameEnvironment().getItemManager().getItem("party_ball");
        if (baseItem == null) return null;
        RoomTile tile = owner.getRoomUnit().getCurrentLocation();
        HabboItem ball = new InteractionDefault(
                TRANSIENT_ITEM_IDS.getAndIncrement(), owner.getHabboInfo().getId(), baseItem, "1", 0, 0);
        ball.setRoomId(room.getId());
        ball.setX(tile.x);
        ball.setY(tile.y);
        ball.setZ(tile.z + 4.0);
        ball.setRotation(0);
        ball.needsUpdate(false);
        room.sendComposer(new AddFloorItemComposer(ball, "Disco").compose());
        return ball;
    }

    private static List<QuakeItem> captureQuakeItems(Room room) {
        List<HabboItem> furni = new ArrayList<>(room.getFloorItems());
        furni.sort(Comparator.comparingInt(HabboItem::getId));
        int count = Math.min(MAX_EARTHQUAKE_FURNI, furni.size());
        List<QuakeItem> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            HabboItem item = furni.get((i * furni.size()) / count);
            RoomTile original = room.getLayout().getTile(item.getX(), item.getY());
            if (original == null) continue;
            RoomTile shake = nearbyTile(room, original, i);
            result.add(new QuakeItem(item, original, shake, item.getZ()));
        }
        return result;
    }

    private static RoomTile nearbyTile(Room room, RoomTile original, int index) {
        int[][] directions = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
        for (int attempt = 0; attempt < directions.length; attempt++) {
            int[] direction = directions[(index + attempt) % directions.length];
            RoomTile tile = room.getLayout().getTile(
                    (short) (original.x + direction[0]), (short) (original.y + direction[1]));
            if (tile != null && tile.state != RoomTileState.INVALID) return tile;
        }
        return original;
    }

    private static RoomTile midpointTile(Room room, RoomTile first, RoomTile second) {
        double targetX = (first.x + second.x) / 2.0;
        double targetY = (first.y + second.y) / 2.0;
        RoomTile nearest = null;
        double bestDistance = Double.MAX_VALUE;
        for (short x = 0; x < room.getLayout().getMapSizeX(); x++) {
            for (short y = 0; y < room.getLayout().getMapSizeY(); y++) {
                RoomTile tile = room.getLayout().getTile(x, y);
                if (tile == null || tile.state == RoomTileState.INVALID) continue;
                double distance = Math.pow(tile.x - targetX, 2) + Math.pow(tile.y - targetY, 2);
                if (distance < bestDistance) {
                    nearest = tile;
                    bestDistance = distance;
                }
            }
        }
        return nearest == null ? first : nearest;
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
        List<RoomTile> orbit = new ArrayList<>();
        Set<Integer> used = new HashSet<>();
        double radiusX = Math.max(1.0, Math.min(4.0, room.getLayout().getMapSizeX() / 3.0));
        double radiusY = Math.max(1.0, Math.min(4.0, room.getLayout().getMapSizeY() / 3.0));
        for (int step = 0; step < 20; step++) {
            double angle = Math.PI * 2.0 * step / 20.0;
            double targetX = center.x + Math.cos(angle) * radiusX;
            double targetY = center.y + Math.sin(angle) * radiusY;
            RoomTile nearest = usable.stream()
                    .min(Comparator.comparingDouble(tile ->
                            Math.pow(tile.x - targetX, 2) + Math.pow(tile.y - targetY, 2)))
                    .orElse(null);
            if (nearest != null && used.add((nearest.x << 16) ^ (nearest.y & 0xFFFF))) orbit.add(nearest);
        }
        return orbit.size() >= 2 ? orbit : usable.subList(0, Math.min(usable.size(), 12));
    }

    private static Long acquire(GameClient gameClient, Room room) {
        Long eventId = RoomFunEventLock.tryAcquire(room);
        if (eventId == null) {
            gameClient.getHabbo().whisper(
                    Emulator.getTexts().getValue("commands.error.cmd_fun_room.active"),
                    RoomChatMessageBubbles.ALERT);
        }
        return eventId;
    }

    private static void broadcast(Room room, String key, String placeholder, String value) {
        String message = Emulator.getTexts().getValue(key).replace(placeholder, value);
        for (Habbo habbo : room.getHabbos()) habbo.whisper(message, RoomChatMessageBubbles.THUNDER);
    }

    private enum Motion {
        NONE,
        BOUNCE,
        LIFT,
        ORBIT
    }

    private enum PairInteraction {
        SWAP(new int[] {4, 4}, 24, RoomChatMessageBubbles.THUNDER),
        MAGNET(new int[] {108, 108}, 26, RoomChatMessageBubbles.THUNDER),
        DUEL(new int[] {33, 34}, 28, RoomChatMessageBubbles.RED),
        HUG(new int[] {9, 9}, 24, RoomChatMessageBubbles.HEARTS),
        TELEPATHY(new int[] {157, 157}, 30, RoomChatMessageBubbles.THUNDER),
        CATAPULT(new int[] {165, 116}, 24, RoomChatMessageBubbles.RED);

        private final int[] effects;
        private final int ticks;
        private final RoomChatMessageBubbles bubble;

        PairInteraction(int[] effects, int ticks, RoomChatMessageBubbles bubble) {
            this.effects = effects;
            this.ticks = ticks;
            this.bubble = bubble;
        }

        int[] effects() {
            return effects;
        }

        int ticks() {
            return ticks;
        }

        RoomChatMessageBubbles bubble() {
            return bubble;
        }
    }

    private record RoomShow(int[] effects, boolean dance, Motion motion, boolean discoBall, boolean freeze) {}

    private record TargetShow(int effect, Motion motion) {}

    private record Atmosphere(
            int[][] palette, String[] landscapes, int baseEffect, int accentEffect) {}

    private record UserState(
            Habbo habbo,
            RoomTile originalTile,
            double originalZ,
            boolean couldWalk,
            int effectId,
            int effectEndTimestamp,
            DanceType danceType) {}

    private record QuakeItem(HabboItem item, RoomTile originalTile, RoomTile shakeTile, double originalZ) {}

    private record CannonLane(RoomTile start, int dx, int dy, int rotation) {}

    private static final class CannonProjectile {
        private final HabboItem item;
        private final int phase;
        private final Habbo homingTarget;
        private int dx;
        private int dy;
        private RoomTile tile;
        private double z;
        private boolean exploded;

        private CannonProjectile(
                HabboItem item,
                RoomTile tile,
                int dx,
                int dy,
                double z,
                int phase,
                Habbo homingTarget) {
            this.item = item;
            this.tile = tile;
            this.dx = dx;
            this.dy = dy;
            this.z = z;
            this.phase = phase;
            this.homingTarget = homingTarget;
        }
    }
}

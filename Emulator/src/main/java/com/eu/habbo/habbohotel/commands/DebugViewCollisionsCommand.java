package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.rooms.items.FurniCollisionOverlayComposer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code :debugviewcollisions} - draw every furni's real collision tiles over the room floor.
 *
 * <p>Answers "is this furni's collision under its picture?" without opening the floor plan editor.
 * The tiles come from {@code RoomLayout.getTilesAt}, which is what the movement checks walk, so a
 * furni drawn three tiles wide but declared 1x1 shows one marker under a picture that covers nine.
 *
 * <p>The overlay is per viewer and lives only in that client: nothing is written to the room.
 * Running the command again clears it, and leaving the room clears it too.
 */
public class DebugViewCollisionsCommand extends Command {

    /** Who is currently looking at the overlay; the client only draws what it was last sent. */
    private static final Set<Integer> WATCHING = ConcurrentHashMap.newKeySet();

    /** Per room, the furni layout the watchers were last sent - see {@link #tick}. */
    private static final Map<Integer, String> LAST_SENT = new ConcurrentHashMap<>();

    public DebugViewCollisionsCommand() {
        super(
                "cmd_debugviewcollisions",
                Emulator.getTexts()
                        .getValue("commands.keys.cmd_debugviewcollisions", "debugviewcollisions;collisions;collisioni")
                        .split(";"));
    }

    public static boolean isWatching(int habboId) {
        return WATCHING.contains(habboId);
    }

    public static void stopWatching(int habboId) {
        WATCHING.remove(habboId);
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        Habbo habbo = gameClient.getHabbo();
        Room room = habbo.getHabboInfo().getCurrentRoom();

        if (room == null) {
            return true;
        }

        if (WATCHING.remove(habbo.getHabboInfo().getId())) {
            gameClient.sendResponse(FurniCollisionOverlayComposer.cleared());
            habbo.whisper(
                    Emulator.getTexts().getValue("commands.generic.collisions.off", "Collisioni: nascoste."),
                    RoomChatMessageBubbles.ALERT);
            return true;
        }

        WATCHING.add(habbo.getHabboInfo().getId());

        Map<HabboItem, Set<RoomTile>> tiles = collect(room);
        LAST_SENT.put(room.getId(), signature(room));
        gameClient.sendResponse(new FurniCollisionOverlayComposer(true, tiles));

        int markers = 0;
        for (Set<RoomTile> occupied : tiles.values()) {
            markers += occupied.size();
        }

        habbo.whisper(
                Emulator.getTexts()
                        .getValue("commands.generic.collisions.on", "Collisioni: %furni% furni, %tiles% caselle.")
                        .replace("%furni%", String.valueOf(tiles.size()))
                        .replace("%tiles%", String.valueOf(markers)),
                RoomChatMessageBubbles.ALERT);

        return true;
    }

    /**
     * Called from the room cycle: when a furni in the room was placed, moved, turned or picked up since the
     * overlay was last sent, every watcher in the room gets a fresh one. The overlay used to be a one-shot
     * snapshot, so it went stale the moment anything moved - the exact moment it is looked at.
     */
    public static void tick(Room room) {
        if (WATCHING.isEmpty() || room == null) {
            return;
        }

        List<Habbo> watchers = new ArrayList<>();

        for (Habbo habbo : room.getHabbos()) {
            if (habbo != null && habbo.getClient() != null && WATCHING.contains(habbo.getHabboInfo().getId())) {
                watchers.add(habbo);
            }
        }

        if (watchers.isEmpty()) {
            LAST_SENT.remove(room.getId());
            return;
        }

        String signature = signature(room);
        String previous = LAST_SENT.put(room.getId(), signature);

        if (signature.equals(previous)) {
            return;
        }

        ServerMessage message = new FurniCollisionOverlayComposer(true, collect(room)).compose();

        for (Habbo habbo : watchers) {
            habbo.getClient().sendResponse(message);
        }
    }

    /** Where every floor furni is, so a change can be noticed without diffing tile sets. */
    private static String signature(Room room) {
        StringBuilder builder = new StringBuilder();

        for (HabboItem item : room.getFloorItems()) {
            if (item == null) continue;

            // The footprint object is replaced whenever items_base is reloaded (Furni Editor save, :update_items), so
            // its identity changing is exactly "the collision shape of this furni changed".
            int footprint = item.getBaseItem() != null ? System.identityHashCode(item.getBaseItem().getFootprint()) : 0;

            builder.append(item.getId()).append(':')
                    .append(item.getX()).append(',')
                    .append(item.getY()).append(',')
                    .append(item.getRotation()).append(',')
                    .append(item.getZ()).append(',')
                    .append(footprint).append(';');
        }

        return builder.toString();
    }

    /** Every floor furni in the room mapped to the tiles the engine says it occupies. */
    public static Map<HabboItem, Set<RoomTile>> collect(Room room) {
        Map<HabboItem, Set<RoomTile>> tiles = new HashMap<>();

        for (HabboItem item : room.getFloorItems()) {
            if (item == null || item.getBaseItem() == null) {
                continue;
            }

            RoomTile origin = room.getLayout().getTile(item.getX(), item.getY());

            if (origin == null) {
                continue;
            }

            Set<RoomTile> occupied =
                    new HashSet<>(room.getLayout().getTilesAt(origin, item.getBaseItem(), item.getRotation()));

            if (!occupied.isEmpty()) {
                tiles.put(item, occupied);
            }
        }

        return tiles;
    }
}

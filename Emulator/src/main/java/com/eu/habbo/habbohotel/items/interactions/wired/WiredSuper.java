package com.eu.habbo.habbohotel.items.interactions.wired;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.ItemManager;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredHighscore;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wired.highscores.WiredHighscoreDataEntry;
import com.eu.habbo.habbohotel.wired.highscores.WiredHighscoreManager;
import gnu.trove.set.hash.THashSet;

import java.util.Collections;
import java.util.List;

// Mikee: Lazy, sorry.
// Mikee: Yeah, duplicate code, sorry.
public class WiredSuper {

    public static void addPoint(final RoomUnit roomUnit, final Room room, final int scoreToAdd) {
        final Habbo habbo = room.getHabbo(roomUnit);

        if (habbo == null) {
            return;
        }

        final ItemManager itemManager = Emulator.getGameEnvironment().getItemManager();
        final WiredHighscoreManager wiredHighscoreManager = itemManager.getHighscoreManager();
        final THashSet<InteractionWiredHighscore> wiredHighscores = room.getRoomSpecialTypes().getWiredHighscores();
        final List<Integer> userIds = Collections.singletonList(habbo.getHabboInfo().getId());

        for (final InteractionWiredHighscore highscore : wiredHighscores) {
            final int itemId = highscore.getId();
            final WiredHighscoreDataEntry entry = new WiredHighscoreDataEntry(itemId, userIds, scoreToAdd, true, Emulator.getIntUnixTimestamp());

            wiredHighscoreManager.addOrUpdateHighscoreData(entry);
        }

        for (final InteractionWiredHighscore highscore : wiredHighscores) {
            highscore.reloadData();

            room.updateItem(highscore);
        }
    }

    public static void setPoint(final RoomUnit roomUnit, final Room room, final int scoreToAdd) {
        final Habbo habbo = room.getHabbo(roomUnit);

        if (habbo == null) {
            return;
        }

        final ItemManager itemManager = Emulator.getGameEnvironment().getItemManager();
        final WiredHighscoreManager wiredHighscoreManager = itemManager.getHighscoreManager();
        final THashSet<InteractionWiredHighscore> wiredHighscores = room.getRoomSpecialTypes().getWiredHighscores();
        final List<Integer> userIds = Collections.singletonList(habbo.getHabboInfo().getId());

        for (final InteractionWiredHighscore highscore : wiredHighscores) {
            final int itemId = highscore.getId();
            final WiredHighscoreDataEntry entry = new WiredHighscoreDataEntry(itemId, userIds, scoreToAdd, true, Emulator.getIntUnixTimestamp());

            wiredHighscoreManager.setHighscoreData(entry);
        }

        for (final InteractionWiredHighscore highscore : wiredHighscores) {
            highscore.reloadData();

            room.updateItem(highscore);
        }
    }

    public static boolean noTotalClassement(RoomUnit roomUnit, Room room) {
        final Habbo habbo = room.getHabbo(roomUnit);

        if (habbo == null) {
            return false;
        }

        final ItemManager itemManager = Emulator.getGameEnvironment().getItemManager();
        final WiredHighscoreManager wiredHighscoreManager = itemManager.getHighscoreManager();
        final THashSet<InteractionWiredHighscore> wiredHighscores = room.getRoomSpecialTypes().getWiredHighscores();
        final List<Integer> userIds = Collections.singletonList(habbo.getHabboInfo().getId());

        for (final InteractionWiredHighscore highscore : wiredHighscores) {
            final WiredHighscoreDataEntry entry = wiredHighscoreManager.getHighscoreRow(highscore.getId(), userIds);

            if (entry != null) {
                return false;
            }
        }

        return true;
    }

    public static boolean totalPointEqual(RoomUnit roomUnit, Room room, int score) {
        final Habbo habbo = room.getHabbo(roomUnit);

        if (habbo == null) {
            return false;
        }

        final ItemManager itemManager = Emulator.getGameEnvironment().getItemManager();
        final WiredHighscoreManager wiredHighscoreManager = itemManager.getHighscoreManager();
        final THashSet<InteractionWiredHighscore> wiredHighscores = room.getRoomSpecialTypes().getWiredHighscores();
        final List<Integer> userIds = Collections.singletonList(habbo.getHabboInfo().getId());

        for (final InteractionWiredHighscore highscore : wiredHighscores) {
            final WiredHighscoreDataEntry entry = wiredHighscoreManager.getHighscoreRow(highscore.getId(), userIds);

            if (entry != null && entry.getScore() == score) {
                return true;
            }
        }

        return false;
    }
}

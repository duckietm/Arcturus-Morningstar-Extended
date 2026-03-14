package com.eu.habbo.habbohotel.wired.highscores;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredHighscore;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.HabboItem;
import gnu.trove.set.hash.THashSet;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

public class WiredHighscoreMidnightUpdater implements Runnable {
    @Override
    public void run() {
        List<Room> rooms = Emulator.getGameEnvironment().getRoomManager().getActiveRooms();

        for (Room room : rooms) {
            if (room == null || room.getRoomSpecialTypes() == null) continue;

            THashSet<HabboItem> items = room.getRoomSpecialTypes().getItemsOfType(InteractionWiredHighscore.class);
            for (HabboItem item : items) {
                ((InteractionWiredHighscore) item).reloadData();
                room.updateItem(item);
            }
        }

        WiredHighscoreManager.midnightUpdater = Emulator.getThreading().run(new WiredHighscoreMidnightUpdater(), getNextUpdaterRun());
    }

    public static int getNextUpdaterRun() {
        return Math.toIntExact(LocalDateTime.now().with(LocalTime.MIDNIGHT).plusDays(1).plusSeconds(-1).atZone(ZoneId.systemDefault()).toEpochSecond() - Emulator.getIntUnixTimestamp()) + 5;
    }
}

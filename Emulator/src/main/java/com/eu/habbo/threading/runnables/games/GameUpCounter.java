package com.eu.habbo.threading.runnables.games;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.interactions.games.InteractionGameUpCounter;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.wired.core.WiredManager;

public class GameUpCounter implements Runnable {
    private final InteractionGameUpCounter timer;

    public GameUpCounter(InteractionGameUpCounter timer) {
        this.timer = timer;
    }

    @Override
    public void run() {
        if (timer.getRoomId() == 0) {
            timer.setRunning(false);
            timer.setThreadActive(false);
            return;
        }

        Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(timer.getRoomId());

        if (room == null || !timer.isRunning() || timer.isPaused()) {
            timer.setThreadActive(false);
            return;
        }

        int tickDelayMs = (int) timer.getNextTickDelayMs();
        timer.advanceCounterInMs(tickDelayMs);
        WiredManager.triggerClockCounter(room, timer);

        if (timer.getCurrentTimeInMs() < timer.getMaximumTimeInMs()) {
            timer.setThreadActive(true);
            Emulator.getThreading().run(this, timer.getNextTickDelayMs());
        } else {
            timer.setThreadActive(false);
            timer.setCurrentTimeInMs(timer.getMaximumTimeInMs());
            timer.endGame(room);
            WiredManager.triggerGameEnds(room);
        }

        room.updateItem(timer);
    }
}

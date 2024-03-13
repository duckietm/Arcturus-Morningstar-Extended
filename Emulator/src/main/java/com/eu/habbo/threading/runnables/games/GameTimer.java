package com.eu.habbo.threading.runnables.games;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.interactions.games.InteractionGameTimer;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;

public class GameTimer implements Runnable {

    private final InteractionGameTimer timer;

    public GameTimer(InteractionGameTimer timer) {
        this.timer = timer;
    }

    @Override
    public void run() {
        if (timer.getRoomId() == 0) {
            timer.setRunning(false);
            return;
        }

        Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(timer.getRoomId());

        if (room == null || !timer.isRunning() || timer.isPaused()) {
            timer.setThreadActive(false);
            return;
        }

        timer.reduceTime();
        if (timer.getTimeNow() < 0) timer.setTimeNow(0);

        if (timer.getTimeNow() > 0) {
            timer.setThreadActive(true);
            Emulator.getThreading().run(this, 1000);
        } else {
            timer.setThreadActive(false);
            timer.setTimeNow(0);
            timer.endGame(room);
            WiredHandler.handle(WiredTriggerType.GAME_ENDS, null, room, new Object[]{});
        }

        room.updateItem(timer);
    }
}

package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.games.Game;
import com.eu.habbo.habbohotel.games.GameState;
import com.eu.habbo.habbohotel.games.wired.WiredGame;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.items.interactions.games.InteractionGameTimer;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomSpecialTypes;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.messages.ServerMessage;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Starts the game in the room ({@code wf_act_game_start} / {@code wf_act_start_timer_game}).
 *
 * <p>Rooms with game timers: every timer that is not already running is started, exactly like the
 * control-clock START action (the timer opens the games, including the wired round, and fires
 * {@code wf_trg_game_starts}). Rooms without a timer: a new wired round is opened directly and
 * {@code wf_trg_game_starts} fires. Already-running rounds are left alone, so the effect cannot
 * loop through its own trigger.</p>
 */
public class WiredEffectGameStart extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.RESET_TIMERS;

    public WiredEffectGameStart(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectGameStart(
            int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public void execute(WiredContext ctx) {
        Room room = ctx == null ? null : ctx.room();
        if (room == null) {
            return;
        }

        boolean hasTimers = false;

        RoomSpecialTypes types = room.getRoomSpecialTypes();
        if (types != null) {
            for (InteractionGameTimer timer : types.getGameTimers().values()) {
                if (timer == null) {
                    continue;
                }

                hasTimers = true;
                timer.startTimer(room); // no-op while running or paused
            }
        }

        if (hasTimers) {
            return;
        }

        Game game = room.getGameOrCreate(WiredGame.class);
        if (!(game instanceof WiredGame) || !GameState.IDLE.equals(game.getState())) {
            return;
        }

        game.initialise();
        WiredManager.triggerGameStarts(room);
    }

    @Deprecated
    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return false;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(5);
        message.appendInt(0);
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(1);
        message.appendInt(this.getDelay());
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());

        if (this.requiresTriggeringUser()) {
            List<Integer> invalidTriggers = new ArrayList<>();
            for (InteractionWiredTrigger object : room.getRoomSpecialTypes().getTriggers(this.getX(), this.getY())) {
                if (!object.isTriggeredByRoomUnit()) {
                    invalidTriggers.add(object.getBaseItem().getSpriteId());
                }
            }
            message.appendInt(invalidTriggers.size());
            for (Integer i : invalidTriggers) {
                message.appendInt(i);
            }
        } else {
            message.appendInt(0);
        }
    }

    @Override
    public boolean saveData(WiredSettings settings, GameClient gameClient) {
        return true;
    }

    @Override
    public String getWiredData() {
        return "";
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {}

    @Override
    public void onPickUp() {}

    @Override
    public WiredEffectType getType() {
        return type;
    }
}

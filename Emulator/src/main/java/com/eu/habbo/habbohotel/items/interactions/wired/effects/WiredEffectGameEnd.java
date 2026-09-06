package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.games.Game;
import com.eu.habbo.habbohotel.games.GameState;
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
 * Ends the game running in the room ({@code wf_act_game_end} / {@code wf_act_end_game} /
 * {@code wf_act_stop_timer_game}).
 *
 * <p>Every running or paused game timer is stopped, which closes the games it drives, and any
 * round still open without a timer (a wired-only game) is closed directly. Closing a game runs
 * {@link Game#onEnd()} - the same path the timer expiry uses - so the {@code items_highscore_data}
 * rows are written and the highscore furni are refreshed. {@code wf_trg_game_ends} fires once when
 * something was actually ended; a second execution (for example from that trigger) is a no-op,
 * so it cannot loop.</p>
 */
public class WiredEffectGameEnd extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.RESET_TIMERS;

    public WiredEffectGameEnd(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectGameEnd(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public void execute(WiredContext ctx) {
        Room room = ctx == null ? null : ctx.room();
        if (room == null) {
            return;
        }

        boolean ended = false;

        RoomSpecialTypes types = room.getRoomSpecialTypes();
        if (types != null) {
            for (InteractionGameTimer timer : types.getGameTimers().values()) {
                if (timer == null || (!timer.isRunning() && !timer.isPaused())) {
                    continue;
                }

                timer.endGame(room);
                room.updateItem(timer);
                timer.needsUpdate(true);
                ended = true;
            }
        }

        for (Game game : room.getGames()) {
            if (game == null || GameState.IDLE.equals(game.getState())) {
                continue;
            }

            game.onEnd();
            game.stop();
            ended = true;
        }

        if (ended) {
            WiredManager.triggerGameEnds(room);
        }
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

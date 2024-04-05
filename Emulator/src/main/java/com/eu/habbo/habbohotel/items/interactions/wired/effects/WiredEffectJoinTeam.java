package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.games.Game;
import com.eu.habbo.habbohotel.games.GameTeamColors;
import com.eu.habbo.habbohotel.games.wired.WiredGame;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import gnu.trove.procedure.TObjectProcedure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WiredEffectJoinTeam extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.JOIN_TEAM;

    private GameTeamColors teamColor = GameTeamColors.RED;

    public WiredEffectJoinTeam(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectJoinTeam(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        Habbo habbo = room.getHabbo(roomUnit);

        if (habbo != null) {
            WiredGame game = (WiredGame) room.getGameOrCreate(WiredGame.class);

            if (habbo.getHabboInfo().getGamePlayer() != null && habbo.getHabboInfo().getCurrentGame() != null && (habbo.getHabboInfo().getCurrentGame() != WiredGame.class || (habbo.getHabboInfo().getCurrentGame() == WiredGame.class && habbo.getHabboInfo().getGamePlayer().getTeamColor() != this.teamColor))) {
                // remove from current game
                Game currentGame = room.getGame(habbo.getHabboInfo().getCurrentGame());
                currentGame.removeHabbo(habbo);
            }

            if(habbo.getHabboInfo().getGamePlayer() == null) {
                game.addHabbo(habbo, this.teamColor);
            }

            return true;
        }

        return false;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(this.teamColor, this.getDelay()));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");

        if(wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            this.setDelay(data.delay);
            this.teamColor = data.team;
        }
        else {
            String[] data = set.getString("wired_data").split("\t");

            if (data.length >= 1) {
                this.setDelay(Integer.valueOf(data[0]));

                if (data.length >= 2) {
                    this.teamColor = GameTeamColors.values()[Integer.valueOf(data[1])];
                }
            }

            this.needsUpdate(true);
        }
    }

    @Override
    public void onPickUp() {
        this.teamColor = GameTeamColors.RED;
        this.setDelay(0);
    }

    @Override
    public WiredEffectType getType() {
        return type;
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
        message.appendInt(this.teamColor.type);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());

        if (this.requiresTriggeringUser()) {
            List<Integer> invalidTriggers = new ArrayList<>();
            room.getRoomSpecialTypes().getTriggers(this.getX(), this.getY()).forEach(new TObjectProcedure<InteractionWiredTrigger>() {
                @Override
                public boolean execute(InteractionWiredTrigger object) {
                    if (!object.isTriggeredByRoomUnit()) {
                        invalidTriggers.add(object.getBaseItem().getSpriteId());
                    }
                    return true;
                }
            });
            message.appendInt(invalidTriggers.size());
            for (Integer i : invalidTriggers) {
                message.appendInt(i);
            }
        } else {
            message.appendInt(0);
        }
    }

    @Override
    public boolean saveData(WiredSettings settings, GameClient gameClient) throws WiredSaveException {
        if(settings.getIntParams().length < 1) throw new WiredSaveException("invalid data");

        int team = settings.getIntParams()[0];

        if(team < 1 || team > 4)
            throw new WiredSaveException("Team is invalid");

        int delay = settings.getDelay();

        if(delay > Emulator.getConfig().getInt("hotel.wired.max_delay", 20))
            throw new WiredSaveException("Delay too long");

        this.teamColor = GameTeamColors.values()[team];
        this.setDelay(delay);

        return true;
    }

    @Override
    public boolean requiresTriggeringUser() {
        return true;
    }

    static class JsonData {
        GameTeamColors team;
        int delay;

        public JsonData(GameTeamColors team, int delay) {
            this.team = team;
            this.delay = delay;
        }
    }
}

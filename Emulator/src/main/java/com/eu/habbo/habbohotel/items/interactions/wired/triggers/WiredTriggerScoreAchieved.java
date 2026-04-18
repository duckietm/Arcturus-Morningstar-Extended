package com.eu.habbo.habbohotel.items.interactions.wired.triggers;

import com.eu.habbo.habbohotel.games.GameTeamColors;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;
import com.eu.habbo.habbohotel.wired.core.WiredEvent;
import com.eu.habbo.messages.ServerMessage;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredTriggerScoreAchieved extends InteractionWiredTrigger {
    private static final WiredTriggerType type = WiredTriggerType.SCORE_ACHIEVED;
    private int score = 0;
    private int teamType = GameTeamColors.NONE.type;

    public WiredTriggerScoreAchieved(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredTriggerScoreAchieved(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean matches(HabboItem triggerItem, WiredEvent event) {
        int points = event.getScore();
        int amountAdded = event.getScoreAdded();

        // Check if this score addition crossed the threshold
        if (!(points - amountAdded < this.score && points >= this.score)) {
            return false;
        }

        if (this.teamType == GameTeamColors.NONE.type) {
            return true;
        }

        if (!event.getActor().isPresent()) {
            return false;
        }

        Habbo habbo = event.getRoom().getHabbo(event.getActor().get());

        return habbo != null
                && habbo.getHabboInfo().getGamePlayer() != null
                && habbo.getHabboInfo().getGamePlayer().getTeamColor().type == this.teamType;
    }

    @Deprecated
    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return false;
    }

    @Override
    public String getWiredData() {
        return WiredManager.getGson().toJson(new JsonData(
            this.score,
            this.teamType
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");

        if (wiredData.startsWith("{")) {
            JsonData data = WiredManager.getGson().fromJson(wiredData, JsonData.class);
            this.score = data.score;
            this.teamType = normalizeTeamType(data.teamType);
        } else {
            try {
                this.score = Integer.parseInt(wiredData);
            } catch (Exception e) {
            }
            this.teamType = GameTeamColors.NONE.type;
        }
    }

    @Override
    public void onPickUp() {
        this.score = 0;
        this.teamType = GameTeamColors.NONE.type;
    }

    @Override
    public WiredTriggerType getType() {
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
        message.appendInt(2);
        message.appendInt(this.score);
        message.appendInt(this.teamType);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(0);
        message.appendInt(0);
    }

    @Override
    public boolean saveData(WiredSettings settings) {
        if(settings.getIntParams().length < 1) return false;
        this.score = settings.getIntParams()[0];
        this.teamType = (settings.getIntParams().length > 1)
                ? normalizeTeamType(settings.getIntParams()[1])
                : GameTeamColors.NONE.type;
        return true;
    }

    @Override
    public boolean isTriggeredByRoomUnit() {
        return true;
    }

    private int normalizeTeamType(int value) {
        if (value >= GameTeamColors.RED.type && value <= GameTeamColors.YELLOW.type) {
            return value;
        }

        return GameTeamColors.NONE.type;
    }

    static class JsonData {
        int score;
        int teamType;

        public JsonData(int score, int teamType) {
            this.score = score;
            this.teamType = teamType;
        }
    }
}

package com.eu.habbo.habbohotel.items.interactions.wired.conditions;

import com.eu.habbo.habbohotel.games.GameTeamColors;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wired.WiredConditionType;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.habbohotel.wired.core.WiredSourceUtil;
import com.eu.habbo.messages.ServerMessage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class WiredConditionTeamMember extends InteractionWiredCondition {
    public static final WiredConditionType type = WiredConditionType.ACTOR_IN_TEAM;

    private GameTeamColors teamColor = GameTeamColors.RED;
    private int userSource = WiredSourceUtil.SOURCE_TRIGGER;

    public WiredConditionTeamMember(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredConditionTeamMember(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean evaluate(WiredContext ctx) {
        Room room = ctx.room();
        List<RoomUnit> targets = WiredSourceUtil.resolveUsers(ctx, this.userSource);
        if (targets.isEmpty()) return false;

        for (RoomUnit roomUnit : targets) {
            Habbo habbo = room.getHabbo(roomUnit);
            if (habbo != null && habbo.getHabboInfo().getGamePlayer() != null) {
                if (habbo.getHabboInfo().getGamePlayer().getTeamColor().equals(this.teamColor)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Deprecated
    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return false;
    }

    @Override
    public String getWiredData() {
        return WiredManager.getGson().toJson(new JsonData(
                this.teamColor,
                this.userSource
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        try {
            String wiredData = set.getString("wired_data");

            if (wiredData.startsWith("{")) {
                JsonData data = WiredManager.getGson().fromJson(wiredData, JsonData.class);
                this.teamColor = data.teamColor;
                this.userSource = data.userSource;
            } else {
                if (!wiredData.equals(""))
                    this.teamColor = GameTeamColors.values()[Integer.parseInt(wiredData)];
                this.userSource = WiredSourceUtil.SOURCE_TRIGGER;
            }
        } catch (Exception e) {
            this.teamColor = GameTeamColors.RED;
            this.userSource = WiredSourceUtil.SOURCE_TRIGGER;
        }
    }

    @Override
    public void onPickUp() {
        this.teamColor = GameTeamColors.RED;
        this.userSource = WiredSourceUtil.SOURCE_TRIGGER;
    }

    @Override
    public WiredConditionType getType() {
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
        message.appendInt(this.teamColor.type);
        message.appendInt(this.userSource);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(0);
        message.appendInt(0);
    }

    @Override
    public boolean saveData(WiredSettings settings) {
        if(settings.getIntParams().length < 1) return false;
        this.teamColor = GameTeamColors.values()[settings.getIntParams()[0]];
        int[] params = settings.getIntParams();
        this.userSource = (params.length > 1) ? params[1] : WiredSourceUtil.SOURCE_TRIGGER;

        return true;
    }

    static class JsonData {
        GameTeamColors teamColor;
        int userSource;

        public JsonData(GameTeamColors teamColor, int userSource) {
            this.teamColor = teamColor;
            this.userSource = userSource;
        }
    }
}

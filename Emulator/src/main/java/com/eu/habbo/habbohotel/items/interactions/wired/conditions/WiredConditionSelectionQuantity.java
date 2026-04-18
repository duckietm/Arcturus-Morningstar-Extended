package com.eu.habbo.habbohotel.items.interactions.wired.conditions;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.wired.WiredConditionType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.habbohotel.wired.core.WiredSourceUtil;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.ServerMessage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class WiredConditionSelectionQuantity extends InteractionWiredCondition {
    private static final int COMPARISON_LESS_THAN = 0;
    private static final int COMPARISON_EQUAL = 1;
    private static final int COMPARISON_GREATER_THAN = 2;

    private static final int SOURCE_GROUP_USERS = 0;
    private static final int SOURCE_GROUP_FURNI = 1;

    public static final WiredConditionType type = WiredConditionType.SLC_QUANTITY;

    private int comparison = COMPARISON_EQUAL;
    private int quantity = 0;
    private int sourceGroup = SOURCE_GROUP_USERS;
    private int sourceType = WiredSourceUtil.SOURCE_TRIGGER;

    public WiredConditionSelectionQuantity(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredConditionSelectionQuantity(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
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
        message.appendInt(4);
        message.appendInt(this.comparison);
        message.appendInt(this.quantity);
        message.appendInt(this.sourceGroup);
        message.appendInt(this.sourceType);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(0);
        message.appendInt(0);
    }

    @Override
    public boolean saveData(WiredSettings settings) {
        int[] params = settings.getIntParams();

        this.comparison = (params.length > 0) ? this.normalizeComparison(params[0]) : COMPARISON_EQUAL;
        this.quantity = (params.length > 1) ? this.normalizeQuantity(params[1]) : 0;
        this.sourceGroup = (params.length > 2) ? this.normalizeSourceGroup(params[2]) : SOURCE_GROUP_USERS;
        this.sourceType = (params.length > 3) ? this.normalizeSourceType(this.sourceGroup, params[3]) : WiredSourceUtil.SOURCE_TRIGGER;

        return true;
    }

    @Override
    public boolean evaluate(WiredContext ctx) {
        int count = this.resolveCount(ctx);

        switch (this.comparison) {
            case COMPARISON_LESS_THAN:
                return count < this.quantity;
            case COMPARISON_GREATER_THAN:
                return count > this.quantity;
            default:
                return count == this.quantity;
        }
    }

    @Deprecated
    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return false;
    }

    @Override
    public String getWiredData() {
        return WiredManager.getGson().toJson(new JsonData(
                this.comparison,
                this.quantity,
                this.sourceGroup,
                this.sourceType
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.onPickUp();

        String wiredData = set.getString("wired_data");
        if (wiredData == null || wiredData.isEmpty()) {
            return;
        }

        if (wiredData.startsWith("{")) {
            JsonData data = WiredManager.getGson().fromJson(wiredData, JsonData.class);

            if (data == null) {
                return;
            }

            this.comparison = this.normalizeComparison(data.comparison);
            this.quantity = this.normalizeQuantity(data.quantity);
            this.sourceGroup = this.normalizeSourceGroup(data.sourceGroup);
            this.sourceType = this.normalizeSourceType(this.sourceGroup, data.sourceType);
            return;
        }

        String[] parts = wiredData.split("\t");

        try {
            if (parts.length > 0) {
                this.comparison = this.normalizeComparison(Integer.parseInt(parts[0]));
            }
            if (parts.length > 1) {
                this.quantity = this.normalizeQuantity(Integer.parseInt(parts[1]));
            }
            if (parts.length > 2) {
                this.sourceGroup = this.normalizeSourceGroup(Integer.parseInt(parts[2]));
            }
            if (parts.length > 3) {
                this.sourceType = this.normalizeSourceType(this.sourceGroup, Integer.parseInt(parts[3]));
            }
        } catch (NumberFormatException ignored) {
            this.onPickUp();
        }
    }

    @Override
    public void onPickUp() {
        this.comparison = COMPARISON_EQUAL;
        this.quantity = 0;
        this.sourceGroup = SOURCE_GROUP_USERS;
        this.sourceType = WiredSourceUtil.SOURCE_TRIGGER;
    }

    private int resolveCount(WiredContext ctx) {
        if (this.sourceGroup == SOURCE_GROUP_FURNI) {
            List<HabboItem> items = WiredSourceUtil.resolveItems(ctx, this.sourceType, null);

            return items.size();
        }

        List<RoomUnit> users = WiredSourceUtil.resolveUsers(ctx, this.sourceType);

        return users.size();
    }

    private int normalizeComparison(int value) {
        switch (value) {
            case COMPARISON_LESS_THAN:
            case COMPARISON_GREATER_THAN:
                return value;
            default:
                return COMPARISON_EQUAL;
        }
    }

    private int normalizeQuantity(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private int normalizeSourceGroup(int value) {
        return (value == SOURCE_GROUP_FURNI) ? SOURCE_GROUP_FURNI : SOURCE_GROUP_USERS;
    }

    private int normalizeSourceType(int group, int value) {
        if (group == SOURCE_GROUP_USERS) {
            return WiredSourceUtil.isDefaultUserSource(value) ? value : WiredSourceUtil.SOURCE_TRIGGER;
        }

        switch (value) {
            case WiredSourceUtil.SOURCE_SELECTOR:
            case WiredSourceUtil.SOURCE_SIGNAL:
            case WiredSourceUtil.SOURCE_TRIGGER:
                return value;
            default:
                return WiredSourceUtil.SOURCE_TRIGGER;
        }
    }

    static class JsonData {
        int comparison;
        int quantity;
        int sourceGroup;
        int sourceType;

        public JsonData(int comparison, int quantity, int sourceGroup, int sourceType) {
            this.comparison = comparison;
            this.quantity = quantity;
            this.sourceGroup = sourceGroup;
            this.sourceType = sourceType;
        }
    }
}

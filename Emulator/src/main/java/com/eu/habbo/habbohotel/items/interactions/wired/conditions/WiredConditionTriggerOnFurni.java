package com.eu.habbo.habbohotel.items.interactions.wired.conditions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredConditionOperator;
import com.eu.habbo.habbohotel.wired.WiredConditionType;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import gnu.trove.set.hash.THashSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class WiredConditionTriggerOnFurni extends InteractionWiredCondition {
    public static final WiredConditionType type = WiredConditionType.TRIGGER_ON_FURNI;

    protected THashSet<HabboItem> items = new THashSet<>();

    public WiredConditionTriggerOnFurni(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredConditionTriggerOnFurni(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (roomUnit == null)
            return false;

        this.refresh();

        if (this.items.isEmpty())
            return false;

        return triggerOnFurni(roomUnit, room);
    }

    protected boolean triggerOnFurni(RoomUnit roomUnit, Room room) {
        THashSet<HabboItem> itemsAtUser = room.getItemsAt(roomUnit.getCurrentLocation());
        return this.items.stream().anyMatch(itemsAtUser::contains);
    }

    @Override
    public String getWiredData() {
        this.refresh();
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
                this.items.stream().map(HabboItem::getId).collect(Collectors.toList())
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.items.clear();
        String wiredData = set.getString("wired_data");

        if (wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);

            for(int id : data.itemIds) {
                HabboItem item = room.getHabboItem(id);

                if (item != null) {
                    this.items.add(item);
                }
            }
        } else {
            String[] data = wiredData.split(";");

            for (String s : data) {
                HabboItem item = room.getHabboItem(Integer.parseInt(s));

                if (item != null) {
                    this.items.add(item);
                }
            }
        }
    }

    @Override
    public void onPickUp() {
        this.items.clear();
    }

    @Override
    public WiredConditionType getType() {
        return type;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        this.refresh();

        message.appendBoolean(false);
        message.appendInt(WiredHandler.MAXIMUM_FURNI_SELECTION);
        message.appendInt(this.items.size());

        for (HabboItem item : this.items)
            message.appendInt(item.getId());

        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(0);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(0);
        message.appendInt(0);
    }

    @Override
    public boolean saveData(WiredSettings settings) {
        int count = settings.getFurniIds().length;
        if (count > Emulator.getConfig().getInt("hotel.wired.furni.selection.count")) return false;

        this.items.clear();

        Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId());

        if (room != null) {
            for (int i = 0; i < count; i++) {
                HabboItem item = room.getHabboItem(settings.getFurniIds()[i]);

                if (item != null) {
                    this.items.add(item);
                }
            }
        }

        return true;
    }

    protected void refresh() {
        THashSet<HabboItem> items = new THashSet<>();

        Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId());
        if (room == null) {
            items.addAll(this.items);
        } else {
            for (HabboItem item : this.items) {
                if (item.getRoomId() != room.getId())
                    items.add(item);
            }
        }

        this.items.removeAll(items);
    }

    @Override
    public WiredConditionOperator operator() {
        return WiredConditionOperator.AND;
    }

    static class JsonData {
        List<Integer> itemIds;

        public JsonData(List<Integer> itemIds) {
            this.itemIds = itemIds;
        }
    }
}

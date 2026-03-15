package com.eu.habbo.habbohotel.items.interactions.wired.triggers;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomSpecialTypes;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;
import com.eu.habbo.habbohotel.wired.core.WiredEvent;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.messages.ServerMessage;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredTriggerReceiveSignal extends InteractionWiredTrigger {
    public static final WiredTriggerType type = WiredTriggerType.RECEIVE_SIGNAL;

    private int channel = 0; // signal channel (0-based)

    public WiredTriggerReceiveSignal(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredTriggerReceiveSignal(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean matches(HabboItem triggerItem, WiredEvent event) {
        return event.getType() == WiredEvent.Type.SIGNAL_RECEIVED
                && event.getSignalChannel() == this.channel;
    }

    public int getChannel() {
        return channel;
    }

    @Override
    public boolean isTriggeredByRoomUnit() {
        return false;
    }

    @Deprecated
    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return false;
    }

    @Override
    public WiredTriggerType getType() {
        return type;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        int senderCount = 0;
        try {
            if (room != null && room.getRoomSpecialTypes() != null) {
                senderCount = room.getRoomSpecialTypes().countSendersTargetingReceiver(this.getId());
            }
        } catch (Exception e) {
        }

        message.appendBoolean(false);
        message.appendInt(0);
        message.appendInt(0);
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(3);
        message.appendInt(channel);
        message.appendInt(senderCount);
        message.appendInt(RoomSpecialTypes.MAX_SENDERS_PER_RECEIVER);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(0);
        message.appendInt(0);
    }

    @Override
    public boolean saveData(WiredSettings settings) {
        int[] params = settings.getIntParams();
        this.channel = params.length > 0 ? params[0] : 0;
        return true;
    }

    @Override
    public String getWiredData() {
        return WiredManager.getGson().toJson(new JsonData(channel));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredManager.getGson().fromJson(wiredData, JsonData.class);
            this.channel = data.channel;
        }
    }

    @Override
    public void onPickUp() {
        this.channel = 0;
    }

    static class JsonData {
        int channel;

        public JsonData() {}

        public JsonData(int channel) {
            this.channel = channel;
        }
    }
}

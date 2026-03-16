package com.eu.habbo.habbohotel.items.interactions.wired.triggers;

import com.eu.habbo.Emulator;
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
import com.eu.habbo.messages.incoming.wired.WiredTriggerSaveException;
import com.eu.habbo.messages.outgoing.rooms.items.ItemStateComposer;
import gnu.trove.set.hash.THashSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class WiredTriggerReceiveSignal extends InteractionWiredTrigger {
    public static final WiredTriggerType type = WiredTriggerType.RECEIVE_SIGNAL;

    private static final String ANTENNA_INTERACTION = "antenna";
    private static final long ACTIVATION_PULSE_MS = 300L;

    private int channel = 0; // signal channel (0-based)
    private THashSet<HabboItem> items;
    private final AtomicLong activationToken = new AtomicLong();

    public WiredTriggerReceiveSignal(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        this.items = new THashSet<>();
    }

    public WiredTriggerReceiveSignal(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
        this.items = new THashSet<>();
    }

    @Override
    public boolean matches(HabboItem triggerItem, WiredEvent event) {
        if (event.getType() != WiredEvent.Type.SIGNAL_RECEIVED) return false;

        if (!this.items.isEmpty()) {
            int signalChannel = event.getSignalChannel();
            for (HabboItem antenna : this.items) {
                if (antenna != null && antenna.getId() == signalChannel) return true;
            }
            return false;
        }

        return event.getSignalChannel() == this.channel;
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
                if (!this.items.isEmpty()) {
                    for (HabboItem item : this.items) {
                        senderCount += room.getRoomSpecialTypes().countSendersTargetingReceiver(item.getId());
                    }
                } else {
                    senderCount = room.getRoomSpecialTypes().countSendersTargetingReceiver(this.getId());
                }
            }
        } catch (Exception e) {
        }

        THashSet<HabboItem> itemsToRemove = new THashSet<>();
        for (HabboItem item : this.items) {
            if (item.getRoomId() != this.getRoomId() || room.getHabboItem(item.getId()) == null) {
                itemsToRemove.add(item);
            }
        }
        for (HabboItem item : itemsToRemove) {
            this.items.remove(item);
        }

        message.appendBoolean(false);
        message.appendInt(WiredManager.MAXIMUM_FURNI_SELECTION);
        message.appendInt(this.items.size());
        for (HabboItem item : this.items) {
            message.appendInt(item.getId());
        }
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
        this.items.clear();

        Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId());
        int count = settings.getFurniIds().length;

        for (int i = 0; i < count; i++) {
            HabboItem item = room.getHabboItem(settings.getFurniIds()[i]);
            if (item == null) continue;
            if (!isAntennaItem(item)) throw new WiredTriggerSaveException("wiredfurni.error.require_antenna_furni");
            this.items.add(item);
        }

        int[] params = settings.getIntParams();
        this.channel = params.length > 0 ? params[0] : 0;
        return true;
    }

    @Override
    public void activateBox(Room room, RoomUnit roomUnit, long millis) {
        if (roomUnit != null) {
            this.addUserExecutionCache(roomUnit.getId(), millis);
        }

        if (room == null || room.isHideWired() || this.getBaseItem().getStateCount() <= 1) {
            return;
        }

        final long token = this.activationToken.incrementAndGet();

        if ("1".equals(this.getExtradata())) {
            this.setExtradata("0");
            room.sendComposer(new ItemStateComposer(this).compose());
        }

        this.setExtradata("1");
        room.sendComposer(new ItemStateComposer(this).compose());

        Emulator.getThreading().run(() -> {
            if (!room.isLoaded()) return;
            if (this.activationToken.get() != token) return;

            this.setExtradata("0");
            room.sendComposer(new ItemStateComposer(this).compose());
        }, ACTIVATION_PULSE_MS);
    }

    @Override
    public String getWiredData() {
        return WiredManager.getGson().toJson(new JsonData(
                channel,
                this.items.stream().map(HabboItem::getId).collect(Collectors.toList())
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.items = new THashSet<>();
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredManager.getGson().fromJson(wiredData, JsonData.class);
            this.channel = data.channel;
            if (data.itemIds != null) {
                for (Integer id : data.itemIds) {
                    HabboItem item = room.getHabboItem(id);
                    if (item != null) this.items.add(item);
                }
            }
        }
    }

    @Override
    public void onPickUp() {
        this.channel = 0;
        this.items.clear();
    }

    private boolean isAntennaItem(HabboItem item) {
        if (item == null || item.getBaseItem() == null || item.getBaseItem().getInteractionType() == null) return false;
        String interaction = item.getBaseItem().getInteractionType().getName();
        if (interaction == null) return false;

        String normalized = interaction.toLowerCase();
        return normalized.equals(ANTENNA_INTERACTION);
    }

    static class JsonData {
        int channel;
        List<Integer> itemIds;

        public JsonData() {}

        public JsonData(int channel, List<Integer> itemIds) {
            this.channel = channel;
            this.itemIds = itemIds;
        }
    }
}

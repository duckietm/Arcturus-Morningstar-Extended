package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSuper;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WiredEffectSuper extends InteractionWiredEffect {

    private static final Logger LOGGER = LoggerFactory.getLogger(WiredEffectSuper.class);
    private static final WiredEffectType WIRED_EFFECT_TYPE = WiredEffectType.BOT_MOVE;
    private static final int CONFIG_MAX_LENGTH = 100;

    private final List<HabboItem> selectedItems = new ArrayList<>();
    private String configKey = "";
    private String configValue = "";

    public WiredEffectSuper(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectSuper(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        LOGGER.debug("Executing super wired effect, key {} value {}", this.configKey, this.configValue);

        try {
            switch (this.configKey) {
                case "addpoint": {
                    final int score = Integer.parseInt(configValue);
                    WiredSuper.addPoint(roomUnit, room, score);
                    break;
                }

                case "setpoint": {
                    final int score = Integer.parseInt(configValue);
                    WiredSuper.setPoint(roomUnit, room, score);
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to run super wired effect " + this.configKey, e);
        }

        return true;
    }

    @Override
    public String getWiredData() {
        ArrayList<Integer> itemIds = new ArrayList<>();

        if (this.selectedItems != null) {
            for (HabboItem item : this.selectedItems) {
                if (item.getRoomId() != 0) {
                    itemIds.add(item.getId());
                }
            }
        }

        return WiredHandler.getGsonBuilder().create().toJson(new WiredEffectSuper.JsonData(this.configKey, this.configValue, itemIds, this.getDelay()));
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        for (final HabboItem item : this.selectedItems) {
            if (item.getRoomId() == this.getRoomId()) continue;
            if (Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId()).getHabboItem(item.getId()) != null) continue;

            this.selectedItems.remove(item);
        }

        message.appendBoolean(false);
        message.appendInt(WiredHandler.MAXIMUM_FURNI_SELECTION);
        message.appendInt(this.selectedItems.size());

        for (final HabboItem item : this.selectedItems) {
            message.appendInt(item.getId());
        }

        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString(this.getConfiguration());
        message.appendInt(0);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());
        message.appendInt(0);
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.selectedItems.clear();

        final String wiredData = set.getString("wired_data");

        if (!wiredData.startsWith("{")) {
            return;
        }

        final WiredEffectSuper.JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, WiredEffectSuper.JsonData.class);

        this.configKey = data.configKey;
        this.configValue = data.configValue;
        this.setDelay(data.delay);

        for (final int itemId : data.items) {
            final HabboItem item = room.getHabboItem(itemId);

            if (item != null) {
                this.selectedItems.add(item);
            }
        }
    }

    @Override
    public void onPickUp() {
        this.selectedItems.clear();
        this.configKey = "";
        this.configValue = "";
        this.setDelay(0);
    }

    @Override
    public boolean saveData(WiredSettings settings, GameClient gameClient) throws WiredSaveException {
        final String configuration = settings.getStringParam();
        final int itemsCount = settings.getFurniIds().length;

        if (itemsCount > Emulator.getConfig().getInt("hotel.wired.furni.selection.count")) {
            throw new WiredSaveException("Too many furni selected");
        }

        final List<HabboItem> newItems = new ArrayList<>();

        for (int i = 0; i < itemsCount; i++) {
            final int itemId = settings.getFurniIds()[i];
            final HabboItem it = Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId()).getHabboItem(itemId);

            if (it == null) {
                throw new WiredSaveException(String.format("Item %s not found", itemId));
            }

            newItems.add(it);
        }

        final int delay = settings.getDelay();

        if (delay > Emulator.getConfig().getInt("hotel.wired.max_delay", 20)) {
            throw new WiredSaveException("Delay too long");
        }

        if (configuration.length() > CONFIG_MAX_LENGTH) {
            return false;
        }

        if (configuration.contains(":")) {
            final String[] parts = configuration.split(":", 2);

            this.configKey = parts[0];
            this.configValue = parts[1];
        } else {
            this.configKey = configuration;
            this.configValue = "";
        }

        this.selectedItems.clear();
        this.selectedItems.addAll(newItems);
        this.setDelay(delay);

        return true;
    }

    @Override
    public WiredEffectType getType() {
        return WIRED_EFFECT_TYPE;
    }

    private String getConfiguration() {
        if ("".equals(this.configKey)) {
            return this.configKey;
        }

        return this.configKey + ":" + this.configValue;
    }

    static class JsonData {
        String configKey;
        String configValue;
        List<Integer> items;
        int delay;

        public JsonData(String configKey, String configValue, List<Integer> items, int delay) {
            this.configKey = configKey;
            this.configValue = configValue;
            this.items = items;
            this.delay = delay;
        }
    }
}

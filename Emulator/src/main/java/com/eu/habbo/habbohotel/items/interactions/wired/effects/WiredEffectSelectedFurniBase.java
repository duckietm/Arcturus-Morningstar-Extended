package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.habbohotel.wired.core.WiredSourceUtil;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Boilerplate for effects that act on a furni selection (manual selection, triggering furni,
 * selector output or signal). Uses the TOGGLE_STATE client dialog (furni picker + furni source), so
 * subclasses only implement {@link #apply(WiredContext, List)}.
 */
public abstract class WiredEffectSelectedFurniBase extends InteractionWiredEffect {
    private final Set<HabboItem> items = new LinkedHashSet<>();
    private int furniSource = WiredSourceUtil.SOURCE_SELECTED;

    protected WiredEffectSelectedFurniBase(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    protected WiredEffectSelectedFurniBase(
            int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    protected abstract void apply(WiredContext ctx, List<HabboItem> targets);

    @Override
    public void execute(WiredContext ctx) {
        if (ctx == null || ctx.room() == null) return;

        List<HabboItem> targets = WiredSourceUtil.resolveItems(ctx, this.furniSource, new ArrayList<>(this.items));
        if (targets.isEmpty()) return;

        this.apply(ctx, targets);
    }

    @Deprecated
    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return false;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        List<HabboItem> validItems = new ArrayList<>();
        for (HabboItem item : new ArrayList<>(this.items)) {
            if (item.getRoomId() == this.getRoomId() && room != null && room.getHabboItem(item.getId()) != null) {
                validItems.add(item);
            } else {
                this.items.remove(item);
            }
        }

        message.appendBoolean(false);
        message.appendInt(WiredManager.MAXIMUM_FURNI_SELECTION);
        message.appendInt(validItems.size());
        for (HabboItem item : validItems) {
            message.appendInt(item.getId());
        }
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(2);
        message.appendInt(0);
        message.appendInt(this.furniSource);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());

        if (this.requiresTriggeringUser() && room != null) {
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
    public boolean saveData(WiredSettings settings, GameClient gameClient) throws WiredSaveException {
        int[] params = settings.getIntParams();
        this.furniSource = params.length > 1 ? params[1] : (params.length > 0 ? params[0] : WiredSourceUtil.SOURCE_SELECTED);

        int itemsCount = settings.getFurniIds().length;
        if (itemsCount > Emulator.getConfig().getInt("hotel.wired.furni.selection.count")) {
            throw new WiredSaveException("Too many furni selected");
        }

        if (itemsCount > 0 && this.furniSource == WiredSourceUtil.SOURCE_TRIGGER) {
            this.furniSource = WiredSourceUtil.SOURCE_SELECTED;
        }

        List<HabboItem> newItems = new ArrayList<>();
        if (this.furniSource == WiredSourceUtil.SOURCE_SELECTED) {
            Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId());
            for (int i = 0; i < itemsCount; i++) {
                int itemId = settings.getFurniIds()[i];
                HabboItem it = room == null ? null : room.getHabboItem(itemId);
                if (it == null) throw new WiredSaveException(String.format("Item %s not found", itemId));
                newItems.add(it);
            }
        }

        int delay = settings.getDelay();
        if (delay > Emulator.getConfig().getInt("hotel.wired.max_delay", 20)) {
            throw new WiredSaveException("Delay too long");
        }

        this.items.clear();
        this.items.addAll(newItems);
        this.setDelay(delay);
        return true;
    }

    @Override
    public String getWiredData() {
        return WiredManager.getGson()
                .toJson(new JsonData(
                        this.getDelay(),
                        this.items.stream().map(HabboItem::getId).collect(Collectors.toList()),
                        this.furniSource));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.items.clear();
        this.furniSource = WiredSourceUtil.SOURCE_SELECTED;
        String wiredData = set.getString("wired_data");
        if (wiredData == null || !wiredData.startsWith("{")) {
            return;
        }

        try {
            JsonData data = WiredManager.getGson().fromJson(wiredData, JsonData.class);
            if (data == null) return;

            this.setDelay(Math.max(0, data.delay));
            this.furniSource = data.furniSource;
            if (data.itemIds != null && room != null) {
                for (Integer id : data.itemIds) {
                    HabboItem item = id == null ? null : room.getHabboItem(id);
                    if (item != null) this.items.add(item);
                }
            }
            if (this.furniSource == WiredSourceUtil.SOURCE_TRIGGER && !this.items.isEmpty()) {
                this.furniSource = WiredSourceUtil.SOURCE_SELECTED;
            }
        } catch (Exception ignored) {
            this.setDelay(0);
        }
    }

    @Override
    public void onPickUp() {
        this.items.clear();
        this.furniSource = WiredSourceUtil.SOURCE_SELECTED;
        this.setDelay(0);
    }

    static class JsonData {
        int delay;
        List<Integer> itemIds;
        int furniSource;

        JsonData(int delay, List<Integer> itemIds, int furniSource) {
            this.delay = delay;
            this.itemIds = itemIds;
            this.furniSource = furniSource;
        }
    }
}

package com.eu.habbo.habbohotel.items.interactions.wired.selector;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class WiredEffectFurniArea extends InteractionWiredEffect {

    public static final WiredEffectType type = WiredEffectType.FURNI_AREA_SELECTOR;

    private int rootX = 0;
    private int rootY = 0;
    private int areaWidth = 0;
    private int areaHeight = 0;

    public WiredEffectFurniArea(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectFurniArea(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public void execute(WiredContext ctx) {
        Room room = ctx.room();
        if (room == null || areaWidth <= 0 || areaHeight <= 0) return;

        List<HabboItem> furniInArea = getFurniInArea(room);
        ctx.targets().setItems(furniInArea);
    }

    private List<HabboItem> getFurniInArea(Room room) {
        List<HabboItem> result = new ArrayList<>();

        int maxX = rootX + areaWidth - 1;
        int maxY = rootY + areaHeight - 1;

        for (int x = rootX; x <= maxX; x++) {
            for (int y = rootY; y <= maxY; y++) {
                for (HabboItem item : room.getItemsAt(x, y)) {
                    if (item != null && !result.contains(item)) {
                        result.add(item);
                    }
                }
            }
        }

        return result;
    }

    @Override
    public boolean saveData(WiredSettings settings, GameClient gameClient) throws WiredSaveException {
        int[] params = settings.getIntParams();
        if (params == null || params.length < 4) {
            throw new WiredSaveException("wf_slc_furni_area requires 4 int params: rootX, rootY, width, height");
        }

        this.rootX = params[0];
        this.rootY = params[1];
        this.areaWidth = params[2];
        this.areaHeight = params[3];
        this.setDelay(settings.getDelay());

        return true;
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }

    @Override
    public String getWiredData() {
        return WiredManager.getGson().toJson(new JsonData(rootX, rootY, areaWidth, areaHeight, getDelay()));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");

        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredManager.getGson().fromJson(wiredData, JsonData.class);
            this.rootX = data.rootX;
            this.rootY = data.rootY;
            this.areaWidth = data.width;
            this.areaHeight = data.height;
            this.setDelay(data.delay);
        }
    }

    @Override
    public void onPickUp() {
        this.rootX = 0;
        this.rootY = 0;
        this.areaWidth = 0;
        this.areaHeight = 0;
        this.setDelay(0);
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {

        message.appendBoolean(false);
        message.appendInt(0);
        message.appendInt(0);

        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(4);
        message.appendInt(this.rootX);
        message.appendInt(this.rootY);
        message.appendInt(this.areaWidth);
        message.appendInt(this.areaHeight);

        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());
        message.appendInt(0);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return false;
    }

    static class JsonData {
        int rootX;
        int rootY;
        int width;
        int height;
        int delay;

        JsonData(int rootX, int rootY, int width, int height, int delay) {
            this.rootX = rootX;
            this.rootY = rootY;
            this.width = width;
            this.height = height;
            this.delay = delay;
        }
    }
}

package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.habbohotel.wired.core.WiredSourceUtil;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserDataComposer;
import com.eu.habbo.messages.outgoing.users.UpdateUserLookComposer;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Undoes {@link WiredEffectGiveLook}: restores the look the user had before the first wired look
 * was applied in this session. Uses the SHOW_MESSAGE dialog (text unused, user source selector).
 */
public class WiredEffectRemoveLook extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.REMOVE_LOOK;

    private int userSource = WiredSourceUtil.SOURCE_TRIGGER;

    public WiredEffectRemoveLook(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectRemoveLook(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public void execute(WiredContext ctx) {
        Room room = ctx.room();
        if (room == null) return;

        for (RoomUnit unit : WiredSourceUtil.resolveUsers(ctx, this.userSource)) {
            Habbo habbo = room.getHabbo(unit);
            if (habbo == null || habbo.getHabboInfo() == null) continue;

            String original = habbo.getHabboInfo().getWiredOriginalLook();
            if (original == null || original.isEmpty()) continue;

            habbo.getHabboInfo().setLook(original);
            habbo.getHabboInfo().setWiredOriginalLook(null);
            if (habbo.getClient() != null) {
                habbo.getClient().sendResponse(new UpdateUserLookComposer(habbo));
            }
            room.sendComposer(new RoomUserDataComposer(habbo).compose());
        }
    }

    @Deprecated
    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return false;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(0);
        message.appendInt(0);
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(1);
        message.appendInt(this.userSource);
        message.appendInt(0);
        message.appendInt(type.code);
        message.appendInt(this.getDelay());
        message.appendInt(0);
    }

    @Override
    public boolean saveData(WiredSettings settings, GameClient gameClient) throws WiredSaveException {
        int[] params = settings.getIntParams();
        this.userSource = params.length > 0 ? params[0] : WiredSourceUtil.SOURCE_TRIGGER;
        this.setDelay(settings.getDelay());
        return true;
    }

    @Override
    public String getWiredData() {
        return WiredManager.getGson().toJson(new JsonData(this.getDelay(), this.userSource));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");
        this.userSource = WiredSourceUtil.SOURCE_TRIGGER;
        if (wiredData == null || !wiredData.startsWith("{")) return;
        try {
            JsonData data = WiredManager.getGson().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.setDelay(Math.max(0, data.delay));
                this.userSource = data.userSource;
            }
        } catch (Exception ignored) {
            this.setDelay(0);
        }
    }

    @Override
    public void onPickUp() {
        this.userSource = WiredSourceUtil.SOURCE_TRIGGER;
        this.setDelay(0);
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }

    static class JsonData {
        int delay;
        int userSource;

        JsonData(int delay, int userSource) {
            this.delay = delay;
            this.userSource = userSource;
        }
    }
}

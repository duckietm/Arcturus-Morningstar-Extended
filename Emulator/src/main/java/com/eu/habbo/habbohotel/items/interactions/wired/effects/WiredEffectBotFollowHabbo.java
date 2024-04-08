package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.bots.Bot;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import gnu.trove.procedure.TObjectProcedure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WiredEffectBotFollowHabbo extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.BOT_FOLLOW_AVATAR;

    private String botName = "";
    private int mode = 0;

    public WiredEffectBotFollowHabbo(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectBotFollowHabbo(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(5);
        message.appendInt(0);
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString(this.botName);
        message.appendInt(1);
        message.appendInt(this.mode);
        message.appendInt(1);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());

        if (this.requiresTriggeringUser()) {
            List<Integer> invalidTriggers = new ArrayList<>();
            room.getRoomSpecialTypes().getTriggers(this.getX(), this.getY()).forEach(new TObjectProcedure<InteractionWiredTrigger>() {
                @Override
                public boolean execute(InteractionWiredTrigger object) {
                    if (!object.isTriggeredByRoomUnit()) {
                        invalidTriggers.add(object.getBaseItem().getSpriteId());
                    }
                    return true;
                }
            });
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
        if(settings.getIntParams().length < 1) throw new WiredSaveException("Mode is invalid");

        int mode = settings.getIntParams()[0];

        if(mode != 0 && mode != 1)
            throw new WiredSaveException("Mode is invalid");

        String botName = settings.getStringParam().replace("\t", "");
        botName = botName.substring(0, Math.min(botName.length(), Emulator.getConfig().getInt("hotel.wired.message.max_length", 100)));

        int delay = settings.getDelay();

        if(delay > Emulator.getConfig().getInt("hotel.wired.max_delay", 20))
            throw new WiredSaveException("Delay too long");

        this.botName = botName;
        this.mode = mode;
        this.setDelay(delay);

        return true;
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        Habbo habbo = room.getHabbo(roomUnit);

        List<Bot> bots = room.getBots(this.botName);

        if (habbo != null && bots.size() == 1) {
            Bot bot = bots.get(0);

            if (this.mode == 1) {
                bot.startFollowingHabbo(habbo);
            } else {
                bot.stopFollowingHabbo();
            }

            return true;
        }

        return false;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(this.botName, this.mode, this.getDelay()));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");

        if(wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            this.setDelay(data.delay);
            this.mode = data.mode;
            this.botName = data.bot_name;
        }
        else {
            String[] data = wiredData.split(((char) 9) + "");

            if (data.length == 3) {
                this.setDelay(Integer.valueOf(data[0]));
                this.mode = (data[1].equalsIgnoreCase("1") ? 1 : 0);
                this.botName = data[2];
            }

            this.needsUpdate(true);
        }
    }

    @Override
    public void onPickUp() {
        this.botName = "";
        this.mode = 0;
        this.setDelay(0);
    }

    @Override
    public boolean requiresTriggeringUser() {
        return true;
    }

    static class JsonData {
        String bot_name;
        int mode;
        int delay;

        public JsonData(String bot_name, int mode, int delay) {
            this.bot_name = bot_name;
            this.mode = mode;
            this.delay = delay;
        }
    }
}

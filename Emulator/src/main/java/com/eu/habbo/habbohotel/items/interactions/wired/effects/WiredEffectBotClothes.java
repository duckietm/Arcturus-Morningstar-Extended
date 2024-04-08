package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.bots.Bot;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

public class WiredEffectBotClothes extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.BOT_CLOTHES;

    private String botName = "";
    private String botLook = "";

    public WiredEffectBotClothes(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectBotClothes(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(5);
        message.appendInt(0);
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString(this.botName + ((char) 9) + "" + this.botLook);
        message.appendInt(0);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());
        message.appendInt(0);
    }

    @Override
    public boolean saveData(WiredSettings settings, GameClient gameClient) throws WiredSaveException {
        String dataString = settings.getStringParam();
        int delay = settings.getDelay();

        if(delay > Emulator.getConfig().getInt("hotel.wired.max_delay", 20))
            throw new WiredSaveException("Delay too long");

        String splitBy = "\t";
        if(!dataString.contains(splitBy))
            throw new WiredSaveException("Malformed data string");

        String[] data = dataString.split(Pattern.quote(splitBy));

        if (data.length != 2)
            throw new WiredSaveException("Malformed data string. Invalid data length");

        this.botName = data[0].substring(0, Math.min(data[0].length(), Emulator.getConfig().getInt("hotel.wired.message.max_length", 100)));
        this.botLook = data[1];
        this.setDelay(delay);

        return true;
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        List<Bot> bots = room.getBots(this.botName);

        if (bots.size() == 1) {
            Bot bot = bots.get(0);
            bot.setFigure(this.botLook);
        }

        return true;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(this.botName, this.botLook, this.getDelay()));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");

        if(wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            this.setDelay(data.delay);
            this.botName = data.bot_name;
            this.botLook = data.look;
        }
        else {
            String[] data = wiredData.split(((char) 9) + "");

            if (data.length >= 3) {
                this.setDelay(Integer.valueOf(data[0]));
                this.botName = data[1];
                this.botLook = data[2];
            }

            this.needsUpdate(true);
        }
    }

    @Override
    public void onPickUp() {
        this.botLook = "";
        this.botName = "";
        this.setDelay(0);
    }

    public String getBotName() {
        return this.botName;
    }

    public void setBotName(String botName) {
        this.botName = botName;
    }

    public String getBotLook() {
        return this.botLook;
    }

    public void setBotLook(String botLook) {
        this.botLook = botLook;
    }

    static class JsonData {
        String bot_name;
        String look;
        int delay;

        public JsonData(String bot_name, String look, int delay) {
            this.bot_name = bot_name;
            this.look = look;
            this.delay = delay;
        }
    }
}

package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.WiredGiveRewardItem;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import com.eu.habbo.messages.outgoing.generic.alerts.UpdateFailedComposer;
import gnu.trove.procedure.TObjectProcedure;
import gnu.trove.set.hash.THashSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WiredEffectGiveReward extends InteractionWiredEffect {
    public final static int LIMIT_ONCE = 0;
    public final static int LIMIT_N_DAY = 1;
    public final static int LIMIT_N_HOURS = 2;
    public final static int LIMIT_N_MINUTES = 3;

    public final static WiredEffectType type = WiredEffectType.GIVE_REWARD;
    public int limit;
    public int limitationInterval;
    public int given;
    public int rewardTime;
    public boolean uniqueRewards;

    public THashSet<WiredGiveRewardItem> rewardItems = new THashSet<>();

    public WiredEffectGiveReward(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectGiveReward(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        Habbo habbo = room.getHabbo(roomUnit);

        return habbo != null && WiredHandler.getReward(habbo, this);
    }

    @Override
    public String getWiredData() {

        ArrayList<WiredGiveRewardItem> rewards = new ArrayList<>(this.rewardItems);
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(this.limit, this.given, this.rewardTime, this.uniqueRewards, this.limitationInterval, rewards, this.getDelay()));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");

        if(wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            this.setDelay(data.delay);
            this.limit = data.limit;
            this.given = data.given;
            this.rewardTime = data.reward_time;
            this.uniqueRewards = data.unique_rewards;
            this.limitationInterval = data.limit_interval;
            this.rewardItems.clear();
            this.rewardItems.addAll(data.rewards);
        }
        else {
            String[] data = wiredData.split(":");
            if (data.length > 0) {
                this.limit = Integer.valueOf(data[0]);
                this.given = Integer.valueOf(data[1]);
                this.rewardTime = Integer.valueOf(data[2]);
                this.uniqueRewards = data[3].equals("1");
                this.limitationInterval = Integer.valueOf(data[4]);
                this.setDelay(Integer.valueOf(data[5]));

                if (data.length > 6) {
                    if (!data[6].equalsIgnoreCase("\t")) {
                        String[] items = data[6].split(";");

                        this.rewardItems.clear();

                        for (String s : items) {
                            try {
                                this.rewardItems.add(new WiredGiveRewardItem(s));
                            } catch (Exception e) {
                            }
                        }
                    }
                }

                this.needsUpdate(true);
            }
        }
    }

    @Override
    public void onPickUp() {
        this.limit = 0;
        this.limitationInterval = 0;
        this.given = 0;
        this.rewardTime = 0;
        this.uniqueRewards = false;
        this.rewardItems.clear();
        this.setDelay(0);
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }

    @Override
    public void onClick(GameClient client, Room room, Object[] objects) throws Exception {
        super.onClick(client, room, objects);

        if (client.getHabbo().hasPermission(Permission.ACC_SUPERWIRED)) {
            client.getHabbo().whisper(Emulator.getTexts().getValue("hotel.wired.superwired.info"), RoomChatMessageBubbles.BOT);
        }
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(this.rewardItems.size());
        message.appendInt(0);
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        StringBuilder s = new StringBuilder();

        for (WiredGiveRewardItem item : this.rewardItems) {
            s.append(item.wiredString()).append(";");
        }
        message.appendString(s.toString());
        message.appendInt(4);
        message.appendInt(this.rewardTime);
        message.appendInt(this.uniqueRewards);
        message.appendInt(this.limit);
        message.appendInt(this.limitationInterval);
        message.appendInt(this.limit > 0);
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
        if (gameClient.getHabbo().hasPermission(Permission.ACC_SUPERWIRED)) {
            if(settings.getIntParams().length < 4) throw new WiredSaveException("Invalid data");
            this.rewardTime = settings.getIntParams()[0];
            this.uniqueRewards = settings.getIntParams()[1] == 1;
            this.limit = settings.getIntParams()[2];
            this.limitationInterval = settings.getIntParams()[3];
            this.given = 0;

            String data = settings.getStringParam();

            String[] items = data.split(";");

            this.rewardItems.clear();

            int i = 1;
            for (String s : items) {
                String[] d = s.split(",");

                if (d.length == 3) {
                    if (!(d[1].contains(":") || d[1].contains(";"))) {
                        this.rewardItems.add(new WiredGiveRewardItem(i, d[0].equalsIgnoreCase("0"), d[1], Integer.valueOf(d[2])));
                        continue;
                    }
                }

                gameClient.sendResponse(new UpdateFailedComposer(Emulator.getTexts().getValue("alert.superwired.invalid")));
                return false;
            }

            this.setDelay(settings.getDelay());

            WiredHandler.dropRewards(this.getId());
            return true;
        }

        gameClient.getHabbo().whisper("U cannot do this.", RoomChatMessageBubbles.ALERT);
        return false;
    }

    @Override
    public boolean requiresTriggeringUser() {
        return true;
    }

    @Override
    protected long requiredCooldown() {
        return 0;
    }

    static class JsonData {
        int limit;
        int given;
        int reward_time;
        boolean unique_rewards;
        int limit_interval;
        List<WiredGiveRewardItem> rewards;
        int delay;

        public JsonData(int limit, int given, int reward_time, boolean unique_rewards, int limit_interval, List<WiredGiveRewardItem> rewards, int delay) {
            this.limit = limit;
            this.given = given;
            this.reward_time = reward_time;
            this.unique_rewards = unique_rewards;
            this.limit_interval = limit_interval;
            this.rewards = rewards;
            this.delay = delay;
        }
    }
}

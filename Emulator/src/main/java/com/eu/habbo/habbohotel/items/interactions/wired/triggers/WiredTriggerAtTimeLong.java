package com.eu.habbo.habbohotel.items.interactions.wired.triggers;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredTriggerReset;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.threading.runnables.WiredExecuteTask;
import gnu.trove.procedure.TObjectProcedure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WiredTriggerAtTimeLong extends InteractionWiredTrigger implements WiredTriggerReset {
    private static final WiredTriggerType type = WiredTriggerType.AT_GIVEN_TIME;
    public int taskId;
    private int executeTime;

    public WiredTriggerAtTimeLong(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredTriggerAtTimeLong(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return true;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
            this.executeTime
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");

        if (wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            this.executeTime = data.executeTime;
        } else {
            if (wiredData.length() >= 1) {
                this.executeTime = (Integer.parseInt(wiredData));
            }
        }

        if (this.executeTime < 500) {
            this.executeTime = 20 * 500;
        }
        this.taskId = 1;
        Emulator.getThreading().run(new WiredExecuteTask(this, Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId())), this.executeTime);
    }

    @Override
    public void onPickUp() {
        this.executeTime = 0;
        this.taskId = 0;
    }

    @Override
    public WiredTriggerType getType() {
        return type;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(5);
        message.appendInt(0);
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(1);
        message.appendInt(this.executeTime / 500);
        message.appendInt(1);
        message.appendInt(this.getType().code);

        if (!this.isTriggeredByRoomUnit()) {
            List<Integer> invalidTriggers = new ArrayList<>();
            room.getRoomSpecialTypes().getEffects(this.getX(), this.getY()).forEach(new TObjectProcedure<InteractionWiredEffect>() {
                @Override
                public boolean execute(InteractionWiredEffect object) {
                    if (object.requiresTriggeringUser()) {
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
    public boolean saveData(WiredSettings settings) {
        if(settings.getIntParams().length < 1) return false;
        this.executeTime = settings.getIntParams()[0] * 500;

        return true;
    }

    @Override
    public void resetTimer() {
        this.taskId++;

        Emulator.getThreading().run(new WiredExecuteTask(this, Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId())), this.executeTime);
    }

    static class JsonData {
        int executeTime;

        public JsonData(int executeTime) {
            this.executeTime = executeTime;
        }
    }
}

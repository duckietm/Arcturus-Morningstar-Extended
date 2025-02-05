package com.eu.habbo.habbohotel.items.interactions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.threading.runnables.CannonKickAction;
import com.eu.habbo.threading.runnables.CannonResetCooldownAction;

import java.sql.ResultSet;
import java.sql.SQLException;

public class InteractionCannon extends HabboItem {
    public boolean cooldown = false;

    public InteractionCannon(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        this.setExtradata("0");
    }

    public InteractionCannon(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
        this.setExtradata("0");
    }

    @Override
    public void serializeExtradata(ServerMessage serverMessage) {
        serverMessage.appendInt((this.isLimited() ? 256 : 0));
        serverMessage.appendString(this.getExtradata());

        super.serializeExtradata(serverMessage);
    }

    @Override
    public boolean canWalkOn(RoomUnit roomUnit, Room room, Object[] objects) {
        return true;
    }

    @Override
    public boolean isWalkable() {
        return false;
    }

    @Override
    public void onClick(GameClient client, Room room, Object[] objects) throws Exception {
        if (room == null)
            return;

        if (client != null) {
            RoomUnit roomUnit = client.getHabbo().getRoomUnit();
            int rotation = this.getRotation();
            int dx = (rotation == 4) ? -1 : (rotation == 0) ? 2 : 0;
            int dy = (rotation == 2) ? 2 : (rotation == 6) ? -1 : 0;
            int x = this.getX() + dx;
            int y = this.getY() + dy;
            if (roomUnit.getX() == x && roomUnit.getY() == y) {
                this.shoot(room, client);
            }
        } else {
            this.shoot(room, null);
        }
    }

    private void shoot(Room room, GameClient client) {
        this.cooldown = true;
        this.setExtradata(this.getExtradata().equals("1") ? "0" : "1");
        room.updateItemState(this);
        Emulator.getThreading().run(new CannonKickAction(this, room, client), 750);
        Emulator.getThreading().run(new CannonResetCooldownAction(this), 2000);
    }

    @Override
    public void onWalk(RoomUnit roomUnit, Room room, Object[] objects) throws Exception {

    }

    @Override
    public void onWalkOn(RoomUnit roomUnit, Room room, Object[] objects) throws Exception {
        super.onWalkOn(roomUnit, room, objects);
    }

    @Override
    public void onWalkOff(RoomUnit roomUnit, Room room, Object[] objects) throws Exception {
        super.onWalkOff(roomUnit, room, objects);
    }

    @Override
    public void onPickUp(Room room) {
        this.setExtradata("0");
    }


    @Override
    public boolean isUsable() {
        return true;
    }
}

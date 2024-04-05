package com.eu.habbo.habbohotel.items.interactions.wired.extra;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredExtra;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.wired.WiredAddonType;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WiredExtraUnseen extends InteractionWiredExtra {
    public List<Integer> seenList = new ArrayList<>();

    public WiredExtraUnseen(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredExtraUnseen(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean saveData(ClientMessage packet) {
        return true;
    }

    @Override
    public WiredAddonType getType() {
        return null;
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return false;
    }

    @Override
    public String getWiredData() {
        return null;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {

    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {

    }

    @Override
    public void onPickUp() {
        this.seenList.clear();
    }

    @Override
    public void onWalk(RoomUnit roomUnit, Room room, Object[] objects) throws Exception {

    }

    @Override
    public void onMove(Room room, RoomTile oldLocation, RoomTile newLocation) {
        super.onMove(room, oldLocation, newLocation);
        this.seenList.clear();
    }

    public InteractionWiredEffect getUnseenEffect(List<InteractionWiredEffect> effects) {
        List<InteractionWiredEffect> unseenEffects = new ArrayList<>();
        for (InteractionWiredEffect effect : effects) {
            if (!this.seenList.contains(effect.getId())) {
                unseenEffects.add(effect);
            }
        }

        InteractionWiredEffect effect = null;
        if (!unseenEffects.isEmpty()) {
            effect = unseenEffects.get(0);
        } else {
            this.seenList.clear();

            if (!effects.isEmpty()) {
                effect = effects.get(0);
            }
        }

        if (effect != null) {
            this.seenList.add(effect.getId());
        }
        return effect;
    }
}
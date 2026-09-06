package com.eu.habbo.messages.incoming.rooms.items;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.interactions.InteractionWired;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.incoming.MessageHandler;

/**
 * Build tool: set an exact state on a furni (extradata), wrapped to the item's state count.
 * Rights holders only; wired furni and furni with non-numeric extradata are left alone.
 */
public class SetFurnitureStateEvent extends MessageHandler {
    private static final int MAX_STATE = 999;

    @Override
    public int getRatelimit() {
        return 100;
    }

    @Override
    public void handle() throws Exception {
        Room room = currentRoom();
        if (room == null) return;

        int itemId = this.packet.readInt();
        int state = this.packet.readInt();
        if (!RoomItemInputGuard.isPositiveId(itemId)) return;

        if (!room.hasRights(this.client.getHabbo()) && !this.client.getHabbo().hasPermission(Permission.ACC_ANYROOMOWNER)) return;

        HabboItem item = room.getHabboItem(itemId);
        if (item == null || item instanceof InteractionWired) return;

        String extradata = item.getExtradata() == null ? "" : item.getExtradata().trim();
        if (!extradata.isEmpty() && !extradata.matches("-?\\d{1,6}")) return;

        // the client only offers the states the furni asset really has; do not wrap to the DB
        // interaction count (it is 1-2 for gates, dice, vending machines, beds...)
        // only the range this furni really uses: the asset's state count (audit) or the DB count
        int allowed = Math.max(item.getBaseItem().getAssetStates(), item.getBaseItem().getStateCount());
        if (allowed < 1) allowed = 1;
        if (state < 0 || state >= allowed || state > MAX_STATE) return;

        item.setExtradata(String.valueOf(state));
        item.needsUpdate(true);
        room.updateItem(item);
        Emulator.getThreading().run(item);
    }
}

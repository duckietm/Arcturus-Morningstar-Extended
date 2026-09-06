package com.eu.habbo.messages.incoming.invsee;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.invsee.InvseeService;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.users.HabboManager;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.inventory.AddHabboItemComposer;
import com.eu.habbo.messages.outgoing.inventory.InventoryRefreshComposer;
import com.eu.habbo.messages.outgoing.inventory.RemoveHabboItemComposer;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Staff inventory inspection, the other direction of {@link InvseeTakeItemEvent}: one item from the acting staff
 * member's own hand is dropped into the target's hand (drag from the inventory window onto the invsee window).
 * The DB row is the source of truth; the in-memory inventories are updated for whoever is online.
 */
public class InvseeGiveItemEvent extends MessageHandler {
    private static final String TRANSFER_SQL =
            "UPDATE items SET user_id = ? WHERE id = ? AND user_id = ? AND room_id = 0 LIMIT 1";

    @Override
    public int getRatelimit() {
        return 250;
    }

    @Override
    public void handle() throws Exception {
        Habbo staff = this.client.getHabbo();
        if (staff == null || !staff.hasPermission(InvseeService.PERMISSION_KEY)) return;

        int targetUserId = this.packet.readInt();
        int itemId = this.packet.readInt();
        if (targetUserId <= 0 || itemId <= 0 || targetUserId == staff.getHabboInfo().getId()) return;

        HabboInfo target = HabboManager.getOfflineHabboInfo(targetUserId);
        if (target == null) return;

        // The item must be in the staff member's hand (not placed in a room, not somebody else's).
        HabboItem given = staff.getInventory().getItemsComponent().getHabboItem(itemId);
        if (given == null || given.getRoomId() != 0) {
            InvseeService.sendInventory(this.client, target);
            return;
        }

        boolean moved = false;
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                PreparedStatement transfer = connection.prepareStatement(TRANSFER_SQL)) {
            transfer.setInt(1, targetUserId);
            transfer.setInt(2, itemId);
            transfer.setInt(3, staff.getHabboInfo().getId());
            moved = transfer.executeUpdate() == 1;
        }

        if (moved) {
            staff.getInventory().getItemsComponent().removeHabboItem(given);
            this.client.sendResponse(new RemoveHabboItemComposer(itemId));
            this.client.sendResponse(new InventoryRefreshComposer());

            Habbo targetOnline = Emulator.getGameEnvironment().getHabboManager().getHabbo(targetUserId);
            if (targetOnline != null) {
                HabboItem fresh = Emulator.getGameEnvironment().getItemManager().loadHabboItem(itemId);
                if (fresh != null) {
                    targetOnline.getInventory().getItemsComponent().addItem(fresh);
                    targetOnline.getClient().sendResponse(new AddHabboItemComposer(fresh));
                    targetOnline.getClient().sendResponse(new InventoryRefreshComposer());
                }
            }
        }

        InvseeService.sendInventory(this.client, target);
    }
}

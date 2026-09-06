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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Staff inventory inspection: move items from the target's hand into the acting staff member's inventory.
 *
 * <p>Wire: {@code targetUserId, baseItemId [, amount, leaveBehind]}. {@code baseItemId} 0 means "every type",
 * {@code amount} 0 means "as many as possible", and {@code leaveBehind} keeps that many pieces of each type with the
 * target — so the client can offer one piece, a whole stack, everything, or everything but one. The two trailing
 * fields are optional so an older client (one item at a time) still works.
 *
 * <p>The DB rows are the source of truth; in-memory inventories are updated for whoever is online.
 */
public class InvseeTakeItemEvent extends MessageHandler {
    /** Upper bound for a single request, so a confiscation cannot lock the items table for thousands of rows. */
    private static final int MAXIMUM_PER_REQUEST = 500;

    private static final String PICK_ONE_TYPE_SQL =
            "SELECT id FROM items WHERE user_id = ? AND room_id = 0 AND item_id = ? ORDER BY id ASC";
    private static final String PICK_EVERYTHING_SQL =
            "SELECT id, item_id FROM items WHERE user_id = ? AND room_id = 0 ORDER BY item_id ASC, id ASC";
    private static final String TRANSFER_SQL =
            "UPDATE items SET user_id = ? WHERE id = ? AND user_id = ? AND room_id = 0";

    @Override
    public int getRatelimit() {
        return 250;
    }

    @Override
    public void handle() throws Exception {
        Habbo staff = this.client.getHabbo();
        if (staff == null || !staff.hasPermission(InvseeService.PERMISSION_KEY)) return;

        int targetUserId = this.packet.readInt();
        int baseItemId = this.packet.readInt();
        int amount = this.packet.bytesAvailable() >= 4 ? this.packet.readInt() : 1;
        int leaveBehind = this.packet.bytesAvailable() >= 4 ? this.packet.readInt() : 0;

        if (targetUserId <= 0 || baseItemId < 0 || targetUserId == staff.getHabboInfo().getId()) return;

        amount = amount <= 0 ? MAXIMUM_PER_REQUEST : Math.min(amount, MAXIMUM_PER_REQUEST);
        leaveBehind = Math.max(0, leaveBehind);

        HabboInfo target = HabboManager.getOfflineHabboInfo(targetUserId);
        if (target == null) return;

        List<Integer> itemIds = pickItems(targetUserId, baseItemId, amount, leaveBehind);
        List<Integer> moved = new ArrayList<>(itemIds.size());

        if (!itemIds.isEmpty()) {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                    PreparedStatement transfer = connection.prepareStatement(TRANSFER_SQL)) {
                for (int itemId : itemIds) {
                    transfer.setInt(1, staff.getHabboInfo().getId());
                    transfer.setInt(2, itemId);
                    transfer.setInt(3, targetUserId);
                    if (transfer.executeUpdate() == 1) moved.add(itemId);
                }
            }
        }

        if (!moved.isEmpty()) {
            Habbo targetOnline = Emulator.getGameEnvironment().getHabboManager().getHabbo(targetUserId);

            for (int itemId : moved) {
                // Keep online caches honest on both sides of the transfer.
                if (targetOnline != null) {
                    HabboItem cached = targetOnline.getInventory().getItemsComponent().getHabboItem(itemId);
                    if (cached != null) targetOnline.getInventory().getItemsComponent().removeHabboItem(cached);
                }

                HabboItem taken = Emulator.getGameEnvironment().getItemManager().loadHabboItem(itemId);
                if (taken == null) continue;

                staff.getInventory().getItemsComponent().addItem(taken);
                this.client.sendResponse(new AddHabboItemComposer(taken));
            }

            if (targetOnline != null) targetOnline.getClient().sendResponse(new InventoryRefreshComposer());
            this.client.sendResponse(new InventoryRefreshComposer());
        }

        InvseeService.sendInventory(this.client, target);
    }

    /**
     * The rows to move: one type or every type, honouring {@code leaveBehind} per type and stopping at
     * {@code amount} rows overall.
     */
    private List<Integer> pickItems(int targetUserId, int baseItemId, int amount, int leaveBehind) throws Exception {
        List<Integer> picked = new ArrayList<>();

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(baseItemId > 0 ? PICK_ONE_TYPE_SQL : PICK_EVERYTHING_SQL)) {
            statement.setInt(1, targetUserId);
            if (baseItemId > 0) statement.setInt(2, baseItemId);

            try (ResultSet set = statement.executeQuery()) {
                // Rows arrive grouped by type, so the "leave N of each type" cut is a per-group counter.
                int currentType = -1;
                int seenInType = 0;

                while (set.next()) {
                    int type = baseItemId > 0 ? baseItemId : set.getInt("item_id");

                    if (type != currentType) {
                        currentType = type;
                        seenInType = 0;
                    }

                    seenInType++;

                    // The first `leaveBehind` of every type stay with the target.
                    if (seenInType <= leaveBehind) continue;

                    picked.add(set.getInt("id"));

                    if (picked.size() >= amount) return picked;
                }
            }
        }

        return picked;
    }
}

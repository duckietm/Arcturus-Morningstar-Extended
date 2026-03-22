package com.eu.habbo.messages.incoming.furnieditor;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.furnieditor.FurniEditorResultComposer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class FurniEditorDeleteEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        if (!this.client.getHabbo().hasPermission(Permission.ACC_CATALOGFURNI)) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "No permission"));
            return;
        }

        int id = this.packet.readInt();

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            // Check usage
            int usageCount = 0;
            try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) as cnt FROM items WHERE item_id = ?")) {
                stmt.setInt(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) usageCount = rs.getInt("cnt");
                }
            }

            if (usageCount > 0) {
                this.client.sendResponse(new FurniEditorResultComposer(false, "Item is in use (" + usageCount + " instances)"));
                return;
            }

            // Get item_name for FurniData removal
            String itemName = null;
            try (PreparedStatement stmt = connection.prepareStatement("SELECT item_name FROM items_base WHERE id = ?")) {
                stmt.setInt(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) itemName = rs.getString("item_name");
                }
            }

            if (itemName == null) {
                this.client.sendResponse(new FurniEditorResultComposer(false, "Item not found"));
                return;
            }

            // Delete catalog items referencing this item
            try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM catalog_items WHERE item_ids = ?")) {
                stmt.setString(1, String.valueOf(id));
                stmt.execute();
            }

            // Delete from items_base
            try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM items_base WHERE id = ?")) {
                stmt.setInt(1, id);
                stmt.execute();
            }

            // Remove from FurnitureData.json
            Emulator.getGameEnvironment().getFurniDataManager().removeEntry(itemName);

            // Refresh item cache
            Emulator.getGameEnvironment().getItemManager().loadItems();

            this.client.sendResponse(new FurniEditorResultComposer(true, "Item deleted"));
        }
    }
}

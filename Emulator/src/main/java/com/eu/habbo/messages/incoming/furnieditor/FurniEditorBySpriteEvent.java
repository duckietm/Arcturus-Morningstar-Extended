package com.eu.habbo.messages.incoming.furnieditor;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.furnieditor.FurniEditorResultComposer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class FurniEditorBySpriteEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        if (!this.client.getHabbo().hasPermission(Permission.ACC_CATALOGFURNI)) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "No permission"));
            return;
        }

        int spriteId = this.packet.readInt();

        if (spriteId <= 0) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "Invalid sprite ID"));
            return;
        }

        // Look up the item ID by sprite_id
        int itemId = -1;

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement stmt = connection.prepareStatement("SELECT id FROM items_base WHERE sprite_id = ? LIMIT 1")) {
            stmt.setInt(1, spriteId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    itemId = rs.getInt("id");
                }
            }
        }

        if (itemId <= 0) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "No item found with sprite_id: " + spriteId));
            return;
        }

        // Delegate to the detail response builder
        FurniEditorDetailEvent.sendDetailResponse(this.client, itemId);
    }
}

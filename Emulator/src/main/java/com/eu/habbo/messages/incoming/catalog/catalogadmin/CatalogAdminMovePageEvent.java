package com.eu.habbo.messages.incoming.catalog.catalogadmin;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.catalog.catalogadmin.CatalogAdminResultComposer;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class CatalogAdminMovePageEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        if (!this.client.getHabbo().hasPermission(Permission.ACC_CATALOGFURNI)) {
            this.client.sendResponse(new CatalogAdminResultComposer(false, "No permission"));
            return;
        }

        int pageId = this.packet.readInt();
        int newParentId = this.packet.readInt();
        int newIndex = this.packet.readInt();

        // Special values: -1 = toggle enabled, -2 = toggle visible
        if (newParentId == -1) {
            // Toggle enabled
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                     "UPDATE catalog_pages SET enabled = IF(enabled = '1', '0', '1') WHERE id = ?")) {
                statement.setInt(1, pageId);
                statement.execute();
            }
            this.client.sendResponse(new CatalogAdminResultComposer(true, "Page toggled"));
            return;
        }

        if (newParentId == -2) {
            // Toggle visible
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                     "UPDATE catalog_pages SET visible = IF(visible = '1', '0', '1') WHERE id = ?")) {
                statement.setInt(1, pageId);
                statement.execute();
            }
            this.client.sendResponse(new CatalogAdminResultComposer(true, "Visibility toggled"));
            return;
        }

        // Normal move: update parent and order
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "UPDATE catalog_pages SET parent_id = ?, order_num = ? WHERE id = ?")) {
            statement.setInt(1, newParentId);
            statement.setInt(2, newIndex);
            statement.setInt(3, pageId);
            statement.execute();
        }

        this.client.sendResponse(new CatalogAdminResultComposer(true, "Page moved"));
    }
}

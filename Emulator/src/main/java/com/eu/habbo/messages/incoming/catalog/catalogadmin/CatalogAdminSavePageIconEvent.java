package com.eu.habbo.messages.incoming.catalog.catalogadmin;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.catalog.CatalogPage;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.catalog.catalogadmin.CatalogAdminResultComposer;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class CatalogAdminSavePageIconEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        if (!this.client.getHabbo().hasPermission(Permission.ACC_CATALOGFURNI)) {
            this.client.sendResponse(new CatalogAdminResultComposer(false, "No permission"));
            return;
        }

        int pageId = this.packet.readInt();
        int iconId = this.packet.readInt();

        CatalogPage page = Emulator.getGameEnvironment().getCatalogManager().catalogPages.get(pageId);

        if (page == null) {
            this.client.sendResponse(new CatalogAdminResultComposer(false, "Page not found: " + pageId));
            return;
        }

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "UPDATE catalog_pages SET icon_image = ? WHERE id = ?")) {
            statement.setInt(1, iconId);
            statement.setInt(2, pageId);
            statement.execute();
        }

        this.client.sendResponse(new CatalogAdminResultComposer(true, "Page icon saved"));
    }
}

package com.eu.habbo.messages.incoming.catalog.catalogadmin;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.catalog.CatalogPage;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.catalog.catalogadmin.CatalogAdminResultComposer;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class CatalogAdminSavePageEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        if (!this.client.getHabbo().hasPermission(Permission.ACC_CATALOGFURNI)) {
            this.client.sendResponse(new CatalogAdminResultComposer(false, "No permission"));
            return;
        }

        int pageId = this.packet.readInt();
        String caption = this.packet.readString();
        String caption2 = this.packet.readString();
        String layout = this.packet.readString();
        int iconType = this.packet.readInt();
        int minRank = this.packet.readInt();
        boolean visible = this.packet.readBoolean();
        boolean enabled = this.packet.readBoolean();
        int orderNum = this.packet.readInt();
        int parentId = this.packet.readInt();
        String headline = this.packet.readString();
        String teaser = this.packet.readString();
        String textDetails = this.packet.readString();

        CatalogPage page = Emulator.getGameEnvironment().getCatalogManager().catalogPages.get(pageId);

        if (page == null) {
            this.client.sendResponse(new CatalogAdminResultComposer(false, "Page not found: " + pageId));
            return;
        }

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "UPDATE catalog_pages SET caption = ?, caption_save = ?, page_layout = ?, icon_image = ?, min_rank = ?, visible = ?, enabled = ?, order_num = ?, parent_id = ?, page_headline = ?, page_teaser = ?, page_text_details = ? WHERE id = ?")) {
            statement.setString(1, caption);
            statement.setString(2, caption2);
            statement.setString(3, layout);
            statement.setInt(4, iconType);
            statement.setInt(5, minRank);
            statement.setString(6, visible ? "1" : "0");
            statement.setString(7, enabled ? "1" : "0");
            statement.setInt(8, orderNum);
            statement.setInt(9, parentId);
            statement.setString(10, headline);
            statement.setString(11, teaser);
            statement.setString(12, textDetails);
            statement.setInt(13, pageId);
            statement.execute();
        }

        this.client.sendResponse(new CatalogAdminResultComposer(true, "Page saved"));
    }
}

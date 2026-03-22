package com.eu.habbo.messages.incoming.catalog.catalogadmin;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.catalog.CatalogPage;
import com.eu.habbo.habbohotel.catalog.CatalogPageLayouts;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.catalog.catalogadmin.CatalogAdminResultComposer;

public class CatalogAdminCreatePageEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        if (!this.client.getHabbo().hasPermission(Permission.ACC_CATALOGFURNI)) {
            this.client.sendResponse(new CatalogAdminResultComposer(false, "No permission"));
            return;
        }

        String caption = this.packet.readString();
        String caption2 = this.packet.readString();
        String layout = this.packet.readString();
        int iconType = this.packet.readInt();
        int minRank = this.packet.readInt();
        boolean visible = this.packet.readBoolean();
        boolean enabled = this.packet.readBoolean();
        int orderNum = this.packet.readInt();
        int parentId = this.packet.readInt();

        CatalogPageLayouts pageLayout;
        try {
            pageLayout = CatalogPageLayouts.valueOf(layout);
        } catch (IllegalArgumentException e) {
            pageLayout = CatalogPageLayouts.default_3x3;
        }

        CatalogPage page = Emulator.getGameEnvironment().getCatalogManager().createCatalogPage(
            caption, caption2, 0, iconType, pageLayout, minRank, parentId
        );

        if (page == null) {
            this.client.sendResponse(new CatalogAdminResultComposer(false, "Failed to create page"));
            return;
        }

        this.client.sendResponse(new CatalogAdminResultComposer(true, "Page created: " + page.getId()));
    }
}

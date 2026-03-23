package com.eu.habbo.messages.incoming.catalog.catalogadmin;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.catalog.catalogadmin.CatalogAdminResultComposer;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class CatalogAdminSaveOfferEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        if (!this.client.getHabbo().hasPermission(Permission.ACC_CATALOGFURNI)) {
            this.client.sendResponse(new CatalogAdminResultComposer(false, "No permission"));
            return;
        }

        int offerId = this.packet.readInt();
        int pageId = this.packet.readInt();
        int itemId = this.packet.readInt();
        String catalogName = this.packet.readString();
        int costCredits = this.packet.readInt();
        int costPoints = this.packet.readInt();
        int pointsType = this.packet.readInt();
        int amount = this.packet.readInt();
        int clubOnly = this.packet.readInt();
        String extradata = this.packet.readString();
        boolean haveOffer = this.packet.readBoolean();
        int offerIdGroup = this.packet.readInt();
        int limitedStack = this.packet.readInt();
        int orderNumber = this.packet.readInt();

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "UPDATE catalog_items SET page_id = ?, item_ids = ?, catalog_name = ?, cost_credits = ?, cost_points = ?, points_type = ?, amount = ?, club_only = ?, extradata = ?, have_offer = ?, offer_id = ?, limited_stack = ?, order_number = ? WHERE id = ?")) {
            statement.setInt(1, pageId);
            statement.setString(2, String.valueOf(itemId));
            statement.setString(3, catalogName);
            statement.setInt(4, costCredits);
            statement.setInt(5, costPoints);
            statement.setInt(6, pointsType);
            statement.setInt(7, amount);
            statement.setString(8, clubOnly == 1 ? "1" : "0");
            statement.setString(9, extradata);
            statement.setString(10, haveOffer ? "1" : "0");
            statement.setInt(11, offerIdGroup);
            statement.setInt(12, limitedStack);
            statement.setInt(13, orderNumber);
            statement.setInt(14, offerId);
            statement.execute();
        }

        this.client.sendResponse(new CatalogAdminResultComposer(true, "Offer saved"));
    }
}

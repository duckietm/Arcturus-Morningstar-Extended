package com.eu.habbo.habbohotel.catalog.layouts;

import com.eu.habbo.habbohotel.catalog.CatalogPage;
import com.eu.habbo.messages.ServerMessage;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Recolourable furni page: the client shows the two colour pickers and sends the chosen colours as the purchase
 * extra data ("RRGGBB,RRGGBB"), which {@link com.eu.habbo.habbohotel.catalog.CatalogManager} turns into the item's
 * custom colours. The wire shape is the plain page shape - only the layout name differs.
 */
public class RecolorableLayout extends CatalogPage {
    public RecolorableLayout(ResultSet set) throws SQLException {
        super(set);
    }

    @Override
    public void serialize(ServerMessage message) {
        message.appendString("recolorable");
        message.appendInt(3);
        message.appendString(super.getHeaderImage());
        message.appendString(super.getTeaserImage());
        message.appendString(super.getSpecialImage());
        message.appendInt(3);
        message.appendString(super.getTextOne());
        message.appendString(super.getTextDetails());
        message.appendString(super.getTextTeaser());
    }
}

package com.eu.habbo.habbohotel.catalog.layouts;

import com.eu.habbo.habbohotel.catalog.CatalogPage;
import com.eu.habbo.messages.ServerMessage;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ClubGiftsLayout extends CatalogPage {
    public ClubGiftsLayout(ResultSet set) throws SQLException {
        super(set);
    }

    @Override
    public void serialize(ServerMessage message) {
        message.appendString("club_gifts");
        message.appendInt(1);
        message.appendString(super.getHeaderImage());
        message.appendInt(1);
        message.appendString(super.getTextOne());
    }
}

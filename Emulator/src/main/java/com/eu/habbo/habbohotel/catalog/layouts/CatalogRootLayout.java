package com.eu.habbo.habbohotel.catalog.layouts;

import com.eu.habbo.habbohotel.catalog.CatalogPage;
import com.eu.habbo.messages.ServerMessage;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CatalogRootLayout extends CatalogPage {
    public CatalogRootLayout() {
        super();

        this.id = -1;
        this.parentId = -2;
        this.rank = 0;
        this.caption = "root";
        this.pageName = "root";
        this.iconColor = 0;
        this.iconImage = 0;
        this.orderNum = -10;
        this.visible = true;
        this.enabled = true;
    }

    public CatalogRootLayout(ResultSet set) throws SQLException {
        super(null);

        this.id = -1;
        this.parentId = -2;
        this.rank = 0;
        this.caption = "root";
        this.pageName = "root";
        this.iconColor = 0;
        this.iconImage = 0;
        this.orderNum = -10;
        this.visible = true;
        this.enabled = true;
    }

    @Override
    public void serialize(ServerMessage message) {

    }
}

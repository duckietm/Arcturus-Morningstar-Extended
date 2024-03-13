package com.eu.habbo.plugin.events.users.catalog;

import com.eu.habbo.habbohotel.catalog.CatalogItem;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.plugin.events.users.UserEvent;

public class UserCatalogEvent extends UserEvent {

    public CatalogItem catalogItem;


    public UserCatalogEvent(Habbo habbo, CatalogItem catalogItem) {
        super(habbo);

        this.catalogItem = catalogItem;
    }
}
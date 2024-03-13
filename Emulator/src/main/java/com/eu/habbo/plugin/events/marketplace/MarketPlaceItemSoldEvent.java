package com.eu.habbo.plugin.events.marketplace;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;

public class MarketPlaceItemSoldEvent extends MarketPlaceEvent {
    public final Habbo seller;
    public final Habbo purchaser;
    public final HabboItem item;
    public int price;

    public MarketPlaceItemSoldEvent(Habbo seller, Habbo purchaser, HabboItem item, int price) {
        this.seller = seller;
        this.purchaser = purchaser;
        this.item = item;
        this.price = price;
    }
}

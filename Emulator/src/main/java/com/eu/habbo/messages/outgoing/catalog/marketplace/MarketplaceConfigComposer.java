package com.eu.habbo.messages.outgoing.catalog.marketplace;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class MarketplaceConfigComposer extends MessageComposer {
    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.MarketplaceConfigComposer);
        this.response.appendBoolean(true);
        this.response.appendInt(1); //Commision Percentage.
        this.response.appendInt(10); //Credits
        this.response.appendInt(5); //Advertisements
        this.response.appendInt(1); //Min price
        this.response.appendInt(1000000); //Max price
        this.response.appendInt(48); //Hours in marketplace
        this.response.appendInt(7); //Days to display
        return this.response;
    }
}

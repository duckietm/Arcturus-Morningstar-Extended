package com.eu.habbo.messages.outgoing.catalog;

import com.eu.habbo.habbohotel.catalog.CatalogItem;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class PurchaseOKComposer extends MessageComposer {
    private final CatalogItem catalogItem;

    public PurchaseOKComposer(CatalogItem catalogItem) {
        this.catalogItem = catalogItem;
    }

    public PurchaseOKComposer() {
        this.catalogItem = null;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.PurchaseOKComposer);
        if (this.catalogItem != null) {
            this.catalogItem.serialize(this.response);
        } else {
            this.response.appendInt(0);
            this.response.appendString("");
            this.response.appendBoolean(false);
            this.response.appendInt(0);
            this.response.appendInt(0);
            this.response.appendInt(0);
            this.response.appendBoolean(true);
            this.response.appendInt(1);
            this.response.appendString("s");
            this.response.appendInt(0);
            this.response.appendString("");
            this.response.appendInt(1);
            this.response.appendInt(0);
            this.response.appendString("");
            this.response.appendInt(1);
        }
        return this.response;
    }
}

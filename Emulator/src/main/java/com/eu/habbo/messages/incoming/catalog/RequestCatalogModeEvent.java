package com.eu.habbo.messages.incoming.catalog;

import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.catalog.CatalogModeComposer;
import com.eu.habbo.messages.outgoing.catalog.CatalogPagesListComposer;

public class RequestCatalogModeEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {

        String MODE = this.packet.readString();
        if (MODE.equalsIgnoreCase("normal")) {
            this.client.sendResponse(new CatalogModeComposer(0));
            this.client.sendResponse(new CatalogPagesListComposer(this.client.getHabbo(), MODE));
        } else {
            this.client.sendResponse(new CatalogModeComposer(1));
            this.client.sendResponse(new CatalogPagesListComposer(this.client.getHabbo(), MODE));
        }

    }
}

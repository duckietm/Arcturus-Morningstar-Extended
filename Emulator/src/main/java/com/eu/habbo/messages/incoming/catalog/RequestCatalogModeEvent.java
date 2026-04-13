package com.eu.habbo.messages.incoming.catalog;

import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.habbohotel.rooms.BuildersClubRoomSupport;
import com.eu.habbo.messages.outgoing.catalog.CatalogPagesListComposer;

public class RequestCatalogModeEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        String MODE = this.packet.readString();
        this.client.sendResponse(new CatalogPagesListComposer(this.client.getHabbo(), MODE));

        if (!MODE.equalsIgnoreCase("normal")) {
            BuildersClubRoomSupport.sendPlacementStatus(this.client.getHabbo());
        }

    }
}

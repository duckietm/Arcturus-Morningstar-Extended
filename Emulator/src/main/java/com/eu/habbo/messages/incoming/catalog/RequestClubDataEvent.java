package com.eu.habbo.messages.incoming.catalog;

import com.eu.habbo.habbohotel.users.subscriptions.SubscriptionHabboClub;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.catalog.ClubCenterDataComposer;
import com.eu.habbo.messages.outgoing.catalog.ClubDataComposer;

public class RequestClubDataEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        this.client.sendResponse(new ClubDataComposer(this.client.getHabbo(), this.packet.readInt()));
    }
}

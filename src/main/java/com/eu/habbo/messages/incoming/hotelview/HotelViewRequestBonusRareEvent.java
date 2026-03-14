package com.eu.habbo.messages.incoming.hotelview;

import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.hotelview.BonusRareComposer;

public class HotelViewRequestBonusRareEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        this.client.sendResponse(new BonusRareComposer(this.client.getHabbo()));
    }
}

package com.eu.habbo.messages.incoming.hotelview;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.hotelview.HallOfFameComposer;
import com.eu.habbo.messages.outgoing.hotelview.HotelViewDataComposer;
import com.eu.habbo.messages.outgoing.hotelview.NewsListComposer;

public class RequestNewsListEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        this.client.sendResponse(new HotelViewDataComposer("2013-05-08 13:0", "gamesmaker"));
        this.client.sendResponse(new HallOfFameComposer(Emulator.getGameEnvironment().getHotelViewManager().getHallOfFame()));
        this.client.sendResponse(new NewsListComposer());
    }
}

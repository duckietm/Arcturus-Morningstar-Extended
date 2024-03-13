package com.eu.habbo.messages.incoming.hotelview;

import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.hotelview.HotelViewDataComposer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HotelViewDataEvent extends MessageHandler {
    @Override
    public void handle() {

        try {
            String data = this.packet.readString();
            if (data.contains(";")) {
                String[] d = data.split(";");

                for (String s : d) {
                    if (s.contains(",")) {
                        this.client.sendResponse(new HotelViewDataComposer(s, s.split(",")[s.split(",").length - 1]));
                    } else {
                        this.client.sendResponse(new HotelViewDataComposer(data, s));
                    }

                    break;
                }

                //this.client.sendResponse(new HotelViewDataComposer("2013-05-08 13:0", "gamesmaker"));
            } else {
                this.client.sendResponse(new HotelViewDataComposer(data, data.split(",")[data.split(",").length - 1]));
            }
        } catch (Exception e) {
            log.error("Caught exception", e);
        }
    }
}

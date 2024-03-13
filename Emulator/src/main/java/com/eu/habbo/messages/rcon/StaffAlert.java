package com.eu.habbo.messages.rcon;

import com.eu.habbo.Emulator;
import com.google.gson.Gson;

public class StaffAlert extends RCONMessage<StaffAlert.JSON> {
    public StaffAlert() {
        super(JSON.class);
    }

    @Override
    public void handle(Gson gson, JSON json) {
        Emulator.getGameEnvironment().getHabboManager().staffAlert(json.message);
    }

    static class JSON {

        public String message;
    }
}

package com.eu.habbo.messages.rcon;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.google.gson.Gson;

public class AlertUser extends RCONMessage<AlertUser.JSONAlertUser> {

    public AlertUser() {
        super(JSONAlertUser.class);
    }

    @Override
    public void handle(Gson gson, JSONAlertUser object) {
        Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(object.user_id);

        if (habbo != null) {
            habbo.alert(object.message);
        }

        this.status = RCONMessage.HABBO_NOT_FOUND;
    }

    static class JSONAlertUser {

        int user_id;


        String message;
    }
}

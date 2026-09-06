package com.eu.habbo.messages.rcon;

import com.eu.habbo.Emulator;
import com.google.gson.Gson;

/** CUSTOM RCON key "updateconfig": the housekeeping applies emulator_settings without a user session. */
public class UpdateConfig extends RCONMessage<UpdateConfig.JSONUpdateConfig> {
    public UpdateConfig() {
        super(JSONUpdateConfig.class);
    }

    @Override
    public void handle(Gson gson, JSONUpdateConfig json) {
        Emulator.getConfig().reload();
    }

    static class JSONUpdateConfig {
    }
}

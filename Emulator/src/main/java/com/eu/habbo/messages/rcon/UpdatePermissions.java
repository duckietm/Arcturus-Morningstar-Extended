package com.eu.habbo.messages.rcon;

import com.eu.habbo.Emulator;
import com.google.gson.Gson;

/** CUSTOM RCON key "updatepermissions": reload permissions and the effect locks (special_enables). */
public class UpdatePermissions extends RCONMessage<UpdatePermissions.JSONUpdatePermissions> {
    public UpdatePermissions() {
        super(JSONUpdatePermissions.class);
    }

    @Override
    public void handle(Gson gson, JSONUpdatePermissions json) {
        Emulator.getGameEnvironment().getPermissionsManager().reload();
    }

    static class JSONUpdatePermissions {
    }
}

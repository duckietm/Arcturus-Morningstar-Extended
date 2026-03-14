package com.eu.habbo.messages.rcon;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.rooms.ForwardToRoomComposer;
import com.google.gson.Gson;

public class StalkUser extends RCONMessage<StalkUser.StalkUserJSON> {
    public StalkUser() {
        super(StalkUserJSON.class);
    }

    @Override
    public void handle(Gson gson, StalkUserJSON json) {
        Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(json.user_id);

        if (habbo != null) {
            Habbo target = Emulator.getGameEnvironment().getHabboManager().getHabbo(json.follow_id);

            if (target == null) {
                this.message = Emulator.getTexts().getValue("commands.error.cmd_stalk.not_found").replace("%user%", json.user_id + "");
                this.status = STATUS_ERROR;
                return;
            }

            if (target.getHabboInfo().getCurrentRoom() == null) {
                this.message = Emulator.getTexts().getValue("commands.error.cmd_stalk.not_room").replace("%user%", json.user_id + "");
                this.status = STATUS_ERROR;
                return;
            }

            if (target.getHabboInfo().getUsername().equals(habbo.getHabboInfo().getUsername())) {
                this.message = Emulator.getTexts().getValue("commands.generic.cmd_stalk.self").replace("%user%", json.user_id + "");
                this.status = STATUS_ERROR;
                return;
            }

            if (target.getHabboInfo().getCurrentRoom() == habbo.getHabboInfo().getCurrentRoom()) {
                this.message = Emulator.getTexts().getValue("commands.generic.cmd_stalk.same_room").replace("%user%", json.user_id + "");
                this.status = STATUS_ERROR;
                return;
            }

            if (this.status == 0) {
                habbo.getClient().sendResponse(new ForwardToRoomComposer(target.getHabboInfo().getCurrentRoom().getId()));
            }
        }
    }

    static class StalkUserJSON {

        public int user_id;


        public int follow_id;
    }
}

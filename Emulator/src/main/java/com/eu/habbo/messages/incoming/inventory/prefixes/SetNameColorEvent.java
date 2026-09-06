package com.eu.habbo.messages.incoming.inventory.prefixes;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.inventory.nickicons.UserNickIconsComposer;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserDataComposer;

/** Header 7020 (custom): string colour ("#RRGGBB" or "" to reset) for the username in bubbles/profile/infostand. */
public class SetNameColorEvent extends MessageHandler {
    @Override
    public int getRatelimit() {
        return 1000;
    }

    @Override
    public void handle() throws Exception {
        Habbo habbo = this.client.getHabbo();

        if (habbo == null || habbo.getInventory() == null || habbo.getInventory().getUserVisualSettingsComponent() == null) {
            return;
        }

        habbo.getInventory().getUserVisualSettingsComponent().setNameColor(this.packet.readString());
        this.client.sendResponse(new UserNickIconsComposer(habbo));

        if (habbo.getHabboInfo().getCurrentRoom() != null) {
            habbo.getHabboInfo().getCurrentRoom().sendComposer(new RoomUserDataComposer(habbo).compose());
        }
    }
}

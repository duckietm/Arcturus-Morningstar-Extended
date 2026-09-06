package com.eu.habbo.messages.incoming.invsee;

import com.eu.habbo.habbohotel.commands.invsee.InvseeService;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.messages.incoming.MessageHandler;

/** Staff inventory inspection: refresh/open the listing for a username. */
public class InvseeRequestEvent extends MessageHandler {
    @Override
    public int getRatelimit() {
        return 500;
    }

    @Override
    public void handle() throws Exception {
        if (this.client.getHabbo() == null
                || !this.client.getHabbo().hasPermission(InvseeService.PERMISSION_KEY)) {
            return;
        }

        String username = this.packet.readString();
        if (username == null || username.isBlank() || username.length() > 32) return;

        HabboInfo target = InvseeService.resolveTarget(username.trim());
        if (target == null) return;

        InvseeService.sendInventory(this.client, target);
    }
}

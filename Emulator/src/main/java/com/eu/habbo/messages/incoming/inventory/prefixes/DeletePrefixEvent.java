package com.eu.habbo.messages.incoming.inventory.prefixes;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.UserPrefix;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.inventory.prefixes.UserPrefixesComposer;

public class DeletePrefixEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int prefixId = this.packet.readInt();

        UserPrefix prefix = this.client.getHabbo().getInventory().getPrefixesComponent().getPrefix(prefixId);

        if (prefix == null) return;

        this.client.getHabbo().getInventory().getPrefixesComponent().removePrefix(prefix);
        prefix.needsDelete(true);
        Emulator.getThreading().run(prefix);

        this.client.sendResponse(new UserPrefixesComposer(this.client.getHabbo()));
    }
}

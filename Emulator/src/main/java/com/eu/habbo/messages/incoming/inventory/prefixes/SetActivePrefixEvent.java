package com.eu.habbo.messages.incoming.inventory.prefixes;

import com.eu.habbo.habbohotel.users.UserPrefix;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.inventory.prefixes.ActivePrefixUpdatedComposer;

public class SetActivePrefixEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int prefixId = this.packet.readInt();

        if (prefixId == 0) {
            this.client.getHabbo().getInventory().getPrefixesComponent().deactivateAll();
            this.client.sendResponse(new ActivePrefixUpdatedComposer(null));
            return;
        }

        UserPrefix prefix = this.client.getHabbo().getInventory().getPrefixesComponent().getPrefix(prefixId);

        if (prefix == null) return;

        this.client.getHabbo().getInventory().getPrefixesComponent().setActive(prefixId);
        this.client.sendResponse(new ActivePrefixUpdatedComposer(prefix));
    }
}

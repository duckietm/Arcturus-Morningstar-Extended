package com.eu.habbo.messages.outgoing.inventory.prefixes;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.UserPrefix;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.List;

public class UserPrefixesComposer extends MessageComposer {
    private final Habbo habbo;

    public UserPrefixesComposer(Habbo habbo) {
        this.habbo = habbo;
    }

    @Override
    protected ServerMessage composeInternal() {
        if (this.habbo == null) return null;

        List<UserPrefix> prefixes = this.habbo.getInventory().getPrefixesComponent().getPrefixes();

        this.response.init(Outgoing.UserPrefixesComposer);
        this.response.appendInt(prefixes.size());

        for (UserPrefix prefix : prefixes) {
            this.response.appendInt(prefix.getId());
            this.response.appendString(prefix.getText());
            this.response.appendString(prefix.getColor());
            this.response.appendString(prefix.getIcon());
            this.response.appendString(prefix.getEffect());
            this.response.appendInt(prefix.isActive() ? 1 : 0);
        }

        return this.response;
    }
}

package com.eu.habbo.messages.outgoing.inventory.prefixes;

import com.eu.habbo.habbohotel.users.UserPrefix;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class ActivePrefixUpdatedComposer extends MessageComposer {
    private final UserPrefix prefix;

    public ActivePrefixUpdatedComposer(UserPrefix prefix) {
        this.prefix = prefix;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.ActivePrefixUpdatedComposer);

        if (this.prefix != null) {
            this.response.appendInt(this.prefix.getId());
            this.response.appendString(this.prefix.getText());
            this.response.appendString(this.prefix.getColor());
            this.response.appendString(this.prefix.getIcon());
            this.response.appendString(this.prefix.getEffect());
        } else {
            this.response.appendInt(0);
            this.response.appendString("");
            this.response.appendString("");
            this.response.appendString("");
            this.response.appendString("");
        }

        return this.response;
    }
}

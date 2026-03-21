package com.eu.habbo.messages.outgoing.inventory.prefixes;

import com.eu.habbo.habbohotel.users.UserPrefix;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class PrefixReceivedComposer extends MessageComposer {
    private final UserPrefix prefix;

    public PrefixReceivedComposer(UserPrefix prefix) {
        this.prefix = prefix;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.PrefixReceivedComposer);
        this.response.appendInt(this.prefix.getId());
        this.response.appendString(this.prefix.getText());
        this.response.appendString(this.prefix.getColor());
        this.response.appendString(this.prefix.getIcon());
        this.response.appendString(this.prefix.getEffect());
        return this.response;
    }
}

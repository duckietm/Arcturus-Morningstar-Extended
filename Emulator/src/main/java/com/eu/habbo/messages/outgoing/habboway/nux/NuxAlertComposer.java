package com.eu.habbo.messages.outgoing.habboway.nux;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class NuxAlertComposer extends MessageComposer {
    private final String link;

    public NuxAlertComposer(String link) {
        this.link = link;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.NuxAlertComposer);
        this.response.appendString(this.link);
        return this.response;
    }
}
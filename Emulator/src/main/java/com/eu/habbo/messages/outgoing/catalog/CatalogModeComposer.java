package com.eu.habbo.messages.outgoing.catalog;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class CatalogModeComposer extends MessageComposer {
    private final int mode;

    public CatalogModeComposer(int mode) {
        this.mode = mode;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.CatalogModeComposer);
        this.response.appendInt(this.mode);
        return this.response;
    }
}

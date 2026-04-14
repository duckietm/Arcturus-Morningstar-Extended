package com.eu.habbo.messages.outgoing.catalog;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class BuildersClubFurniCountComposer extends MessageComposer {
    private final int furniCount;

    public BuildersClubFurniCountComposer(int furniCount) {
        this.furniCount = furniCount;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.CatalogModeComposer);
        this.response.appendInt(this.furniCount);
        return this.response;
    }
}

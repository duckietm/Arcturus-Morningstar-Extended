package com.eu.habbo.messages.outgoing.unknown;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class EpicPopupFrameComposer extends MessageComposer {
    public static final String LIBRARY_URL = "${image.library.url}";
    private final String assetURI;

    public EpicPopupFrameComposer(String assetURI) {
        this.assetURI = assetURI;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.EpicPopupFrameComposer);
        this.response.appendString(this.assetURI);
        return this.response;
    }
}

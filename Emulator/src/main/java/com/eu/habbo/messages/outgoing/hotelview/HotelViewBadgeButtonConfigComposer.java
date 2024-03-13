package com.eu.habbo.messages.outgoing.hotelview;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class HotelViewBadgeButtonConfigComposer extends MessageComposer {
    private final String badge;
    private final boolean enabled;

    public HotelViewBadgeButtonConfigComposer(String badge, boolean enabled) {
        this.badge = badge;
        this.enabled = enabled;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.HotelViewBadgeButtonConfigComposer);
        this.response.appendString(this.badge);
        this.response.appendBoolean(this.enabled);
        return this.response;
    }
}
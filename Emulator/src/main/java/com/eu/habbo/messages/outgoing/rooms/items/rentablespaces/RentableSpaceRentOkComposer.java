package com.eu.habbo.messages.outgoing.rooms.items.rentablespaces;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

/** RentableSpaceRentOk (2046): the rent went through, {@code expiryTime} seconds are left. */
public class RentableSpaceRentOkComposer extends MessageComposer {
    private final int expiryTime;

    public RentableSpaceRentOkComposer(int expiryTime) {
        this.expiryTime = expiryTime;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.RentableSpaceRentOkComposer);
        this.response.appendInt(this.expiryTime);
        return this.response;
    }

    public int getExpiryTime() {
        return expiryTime;
    }
}

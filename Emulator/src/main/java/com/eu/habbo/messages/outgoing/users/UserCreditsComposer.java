package com.eu.habbo.messages.outgoing.users;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class UserCreditsComposer extends MessageComposer {
    private final Habbo habbo;

    public UserCreditsComposer(Habbo habbo) {
        this.habbo = habbo;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.UserCreditsComposer);
        this.response.appendString(this.habbo.getHabboInfo().getCredits() + ".0");
        return this.response;
    }
}

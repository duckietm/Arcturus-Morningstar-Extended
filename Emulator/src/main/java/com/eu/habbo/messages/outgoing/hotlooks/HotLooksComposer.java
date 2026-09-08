package com.eu.habbo.messages.outgoing.hotlooks;

import com.eu.habbo.habbohotel.hotlooks.HotLook;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import java.util.List;

/** AIR 13 HotLooks: int count, then per look string gender ("M"/"F") and string figure. */
public class HotLooksComposer extends MessageComposer {
    private final List<HotLook> looks;

    public HotLooksComposer(List<HotLook> looks) {
        this.looks = looks;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.HotLooksComposer);
        this.response.appendInt(this.looks.size());
        for (HotLook look : this.looks) {
            this.response.appendString(look.gender());
            this.response.appendString(look.figure());
        }
        return this.response;
    }
}

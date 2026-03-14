package com.eu.habbo.messages.outgoing.mysterybox;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class MysteryBoxKeysComposer extends MessageComposer {
    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.MysteryBoxKeysComposer);
        this.response.appendString(""); //Box color
        this.response.appendString(""); //Key color
        return this.response;
    }
}
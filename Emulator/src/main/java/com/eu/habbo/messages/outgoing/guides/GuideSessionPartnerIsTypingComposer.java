package com.eu.habbo.messages.outgoing.guides;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class GuideSessionPartnerIsTypingComposer extends MessageComposer {
    private final boolean typing;

    public GuideSessionPartnerIsTypingComposer(boolean typing) {
        this.typing = typing;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.GuideSessionPartnerIsTypingComposer);
        this.response.appendBoolean(this.typing);
        return this.response;
    }
}

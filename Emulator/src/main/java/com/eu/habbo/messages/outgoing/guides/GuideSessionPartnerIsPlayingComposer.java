package com.eu.habbo.messages.outgoing.guides;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class GuideSessionPartnerIsPlayingComposer extends MessageComposer {
    public final boolean isPlaying;

    public GuideSessionPartnerIsPlayingComposer(boolean isPlaying) {
        this.isPlaying = isPlaying;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.GuideSessionPartnerIsPlayingComposer);
        this.response.appendBoolean(this.isPlaying);
        return this.response;
    }
}
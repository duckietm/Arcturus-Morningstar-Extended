package com.eu.habbo.messages.outgoing.rooms.youtube;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class YouTubeRoomSettingsComposer extends MessageComposer {
    private final boolean youtubeEnabled;

    public YouTubeRoomSettingsComposer(boolean youtubeEnabled) {
        this.youtubeEnabled = youtubeEnabled;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.YouTubeRoomSettingsComposer);
        this.response.appendInt(this.youtubeEnabled ? 1 : 0);
        return this.response;
    }
}

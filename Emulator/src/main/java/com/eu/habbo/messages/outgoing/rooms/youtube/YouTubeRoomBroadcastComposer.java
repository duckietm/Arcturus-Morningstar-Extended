package com.eu.habbo.messages.outgoing.rooms.youtube;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.List;

public class YouTubeRoomBroadcastComposer extends MessageComposer {
    private final String videoId;
    private final String senderName;
    private final List<String> playlist;

    public YouTubeRoomBroadcastComposer(String videoId, String senderName, List<String> playlist) {
        this.videoId = videoId;
        this.senderName = senderName;
        this.playlist = playlist;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.YouTubeRoomBroadcastComposer);
        this.response.appendString(this.videoId);
        this.response.appendString(this.senderName);
        this.response.appendInt(this.playlist.size());
        for (String id : this.playlist) {
            this.response.appendString(id);
        }
        return this.response;
    }
}

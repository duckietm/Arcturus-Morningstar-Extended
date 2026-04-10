package com.eu.habbo.messages.outgoing.rooms.youtube;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.Set;

public class YouTubeRoomWatchersComposer extends MessageComposer {
    private final Set<Integer> watcherIds;

    public YouTubeRoomWatchersComposer(Set<Integer> watcherIds) {
        this.watcherIds = watcherIds;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.YouTubeRoomWatchersComposer);
        this.response.appendInt(this.watcherIds.size());
        for (int id : this.watcherIds) {
            this.response.appendInt(id);
        }
        return this.response;
    }
}

package com.eu.habbo.messages.outgoing.rooms.items.jukebox;

import com.eu.habbo.habbohotel.items.SoundTrack;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class JukeBoxPlayListAddSongComposer extends MessageComposer {
    private final SoundTrack track;

    public JukeBoxPlayListAddSongComposer(SoundTrack track) {
        this.track = track;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.JukeBoxPlayListAddSongComposer);
        this.response.appendInt(this.track.getId());
        this.response.appendInt(this.track.getLength() * 1000);
        this.response.appendString(this.track.getCode());
        this.response.appendString(this.track.getAuthor());
        return this.response;
    }
}

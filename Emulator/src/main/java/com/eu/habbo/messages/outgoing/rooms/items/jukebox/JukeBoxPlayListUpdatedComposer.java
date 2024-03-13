package com.eu.habbo.messages.outgoing.rooms.items.jukebox;

import com.eu.habbo.habbohotel.items.SoundTrack;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import gnu.trove.set.hash.THashSet;

public class JukeBoxPlayListUpdatedComposer extends MessageComposer {
    private final THashSet<SoundTrack> tracks;

    public JukeBoxPlayListUpdatedComposer(THashSet<SoundTrack> tracks) {
        this.tracks = tracks;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.JukeBoxPlayListUpdatedComposer);

        int length = 0;

        for (SoundTrack track : this.tracks) {
            length += track.getLength();
        }

        this.response.appendInt(length * 1000);
        this.response.appendInt(this.tracks.size());

        for (SoundTrack track : this.tracks) {
            this.response.appendInt(track.getId());
            this.response.appendInt(track.getLength() * 1000);
            this.response.appendString(track.getCode());
            this.response.appendString(track.getAuthor());
        }

        return this.response;
    }
}

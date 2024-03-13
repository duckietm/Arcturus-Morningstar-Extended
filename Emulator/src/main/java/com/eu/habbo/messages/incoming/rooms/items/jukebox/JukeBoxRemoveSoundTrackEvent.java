package com.eu.habbo.messages.incoming.rooms.items.jukebox;

import com.eu.habbo.habbohotel.items.interactions.InteractionMusicDisc;
import com.eu.habbo.messages.incoming.MessageHandler;

public class JukeBoxRemoveSoundTrackEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int index = this.packet.readInt();

        InteractionMusicDisc musicDisc = this.client.getHabbo().getHabboInfo().getCurrentRoom().getTraxManager().getSongs().get(index);

        if (musicDisc != null) {
            this.client.getHabbo().getHabboInfo().getCurrentRoom().getTraxManager().removeSong(musicDisc.getId());
        }
    }
}

package com.eu.habbo.messages.incoming.rooms.youtube;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.rooms.youtube.YouTubeRoomWatchersComposer;

public class YouTubeRoomWatchingEvent extends MessageHandler {

    @Override
    public int getRatelimit() {
        return 500;
    }

    @Override
    public void handle() throws Exception {
        Habbo habbo = this.client.getHabbo();
        if (habbo == null) return;

        Room room = habbo.getHabboInfo().getCurrentRoom();
        if (room == null) return;

        boolean watching = this.packet.readInt() == 1;
        int userId = habbo.getHabboInfo().getId();

        boolean changed;
        if (watching) {
            changed = room.getYoutubeWatchers().add(userId);
        } else {
            changed = room.getYoutubeWatchers().remove(userId);
        }

        if (changed) {
            room.sendComposer(
                new YouTubeRoomWatchersComposer(room.getYoutubeWatchers()).compose()
            );
        }
    }
}

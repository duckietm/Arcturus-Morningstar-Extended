package com.eu.habbo.messages.incoming.rooms.youtube;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.rooms.youtube.YouTubeRoomWatchersComposer;

public class YouTubeRoomWatchingEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        Habbo habbo = this.client.getHabbo();
        if (habbo == null) return;

        Room room = habbo.getHabboInfo().getCurrentRoom();
        if (room == null) return;

        boolean watching = this.packet.readBoolean();
        int userId = habbo.getHabboInfo().getId();

        if (watching) {
            room.getYoutubeWatchers().add(userId);
        } else {
            room.getYoutubeWatchers().remove(userId);
        }

        room.sendComposer(
            new YouTubeRoomWatchersComposer(room.getYoutubeWatchers()).compose()
        );
    }
}

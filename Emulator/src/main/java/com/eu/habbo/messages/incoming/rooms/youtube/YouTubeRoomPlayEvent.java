package com.eu.habbo.messages.incoming.rooms.youtube;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.rooms.youtube.YouTubeRoomBroadcastComposer;

import java.util.ArrayList;
import java.util.List;

public class YouTubeRoomPlayEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        Habbo habbo = this.client.getHabbo();
        if (habbo == null) return;

        Room room = habbo.getHabboInfo().getCurrentRoom();
        if (room == null) return;

        // Only the room owner or users with rights can broadcast
        if (!room.isOwner(habbo) && !room.hasRights(habbo)) return;

        String videoId = this.packet.readString();

        int playlistCount = this.packet.readInt();
        List<String> playlist = new ArrayList<>();
        for (int i = 0; i < playlistCount && i < 50; i++) {
            playlist.add(this.packet.readString());
        }

        // Store the current video + playlist on the room
        room.setYoutubeVideo(videoId, habbo.getHabboInfo().getUsername(), playlist);

        // Broadcast to everyone in the room
        room.sendComposer(
            new YouTubeRoomBroadcastComposer(videoId, habbo.getHabboInfo().getUsername(), playlist).compose()
        );
    }
}

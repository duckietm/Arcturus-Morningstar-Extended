package com.eu.habbo.messages.incoming.rooms.youtube;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.rooms.youtube.YouTubeRoomBroadcastComposer;

import java.util.ArrayList;
import java.util.List;

public class YouTubeRoomPlayEvent extends MessageHandler {

    private static final int MAX_VIDEO_ID_LENGTH = 100;
    private static final int MAX_PLAYLIST_ITEM_LENGTH = 200;
    private static final int MAX_PLAYLIST_SIZE = 50;

    @Override
    public int getRatelimit() {
        // Max 1 broadcast every 2 seconds per client
        return 2000;
    }

    @Override
    public void handle() throws Exception {
        Habbo habbo = this.client.getHabbo();
        if (habbo == null) return;

        Room room = habbo.getHabboInfo().getCurrentRoom();
        if (room == null) return;
        if (!room.isYoutubeEnabled()) return;
        if (!room.isOwner(habbo) && !room.hasRights(habbo)) return;

        String videoId = this.packet.readString();
        if (videoId.length() > MAX_VIDEO_ID_LENGTH) {
            videoId = videoId.substring(0, MAX_VIDEO_ID_LENGTH);
        }

        int playlistCount = this.packet.readInt();
        if (playlistCount > MAX_PLAYLIST_SIZE) playlistCount = MAX_PLAYLIST_SIZE;
        if (playlistCount < 0) playlistCount = 0;

        List<String> playlist = new ArrayList<>();
        for (int i = 0; i < playlistCount; i++) {
            String item = this.packet.readString();
            if (item.length() > MAX_PLAYLIST_ITEM_LENGTH) {
                item = item.substring(0, MAX_PLAYLIST_ITEM_LENGTH);
            }
            playlist.add(item);
        }

        // Store the current video + playlist on the room, or clear if empty
        if (videoId.isEmpty()) {
            room.clearYoutubeVideo();
        } else {
            room.setYoutubeVideo(videoId, habbo.getHabboInfo().getUsername(), playlist);
        }

        // Broadcast to everyone in the room (empty videoId = stop)
        room.sendComposer(
            new YouTubeRoomBroadcastComposer(videoId, habbo.getHabboInfo().getUsername(), playlist).compose()
        );
    }
}

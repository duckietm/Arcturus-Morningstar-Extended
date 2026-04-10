package com.eu.habbo.messages.incoming.rooms.youtube;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.rooms.youtube.YouTubeRoomBroadcastComposer;
import com.eu.habbo.messages.outgoing.rooms.youtube.YouTubeRoomSettingsComposer;

public class YouTubeRoomSettingsEvent extends MessageHandler {

    @Override
    public int getRatelimit() {
        return 200;
    }

    @Override
    public void handle() throws Exception {
        Habbo habbo = this.client.getHabbo();
        if (habbo == null) return;

        Room room = habbo.getHabboInfo().getCurrentRoom();
        if (room == null) return;
        if (!room.isOwner(habbo)) return;

        boolean enabled = this.packet.readInt() == 1;
        room.setYoutubeEnabled(enabled);
        room.setNeedsUpdate(true);
        room.sendComposer(new YouTubeRoomSettingsComposer(enabled).compose());

        if (!enabled && !room.getYoutubeCurrentVideo().isEmpty()) {
            room.clearYoutubeVideo();
            room.sendComposer(new YouTubeRoomBroadcastComposer("", "", java.util.Collections.emptyList()).compose());
        }
    }
}

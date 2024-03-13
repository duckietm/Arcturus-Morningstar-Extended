package com.eu.habbo.messages.incoming.rooms.items.youtube;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.YoutubeManager;
import com.eu.habbo.habbohotel.items.interactions.InteractionYoutubeTV;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.rooms.items.youtube.YoutubeVideoComposer;
import com.eu.habbo.threading.runnables.YoutubeAdvanceVideo;

import java.util.Optional;

public class YoutubeRequestPlaylistChange extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int itemId = this.packet.readInt();
        String playlistId = this.packet.readString();

        Habbo habbo = this.client.getHabbo();

        if (habbo == null) return;


        Room room = habbo.getHabboInfo().getCurrentRoom();

        if (room == null) return;
        if (!room.isOwner(habbo) && !habbo.hasPermission(Permission.ACC_ANYROOMOWNER)) return;


        HabboItem item = this.client.getHabbo().getHabboInfo().getCurrentRoom().getHabboItem(itemId);

        if (item == null || !(item instanceof  InteractionYoutubeTV)) return;

        Optional<YoutubeManager.YoutubePlaylist> playlist = Emulator.getGameEnvironment().getItemManager().getYoutubeManager().getPlaylistsForItemId(item.getBaseItem().getId()).stream().filter(p -> p.getId().equals(playlistId)).findAny();

        if (playlist.isPresent()) {
            YoutubeManager.YoutubeVideo video = playlist.get().getVideos().get(0);
            if (video == null) return;

            ((InteractionYoutubeTV) item).currentVideo = video;
            ((InteractionYoutubeTV) item).currentPlaylist = playlist.get();

            ((InteractionYoutubeTV) item).cancelAdvancement();

            room.updateItem(item);
            room.sendComposer(new YoutubeVideoComposer(itemId, video, true, 0).compose());
            ((InteractionYoutubeTV) item).autoAdvance = Emulator.getThreading().run(new YoutubeAdvanceVideo((InteractionYoutubeTV) item), video.getDuration() * 1000);

            item.needsUpdate(true);
        }
    }
}
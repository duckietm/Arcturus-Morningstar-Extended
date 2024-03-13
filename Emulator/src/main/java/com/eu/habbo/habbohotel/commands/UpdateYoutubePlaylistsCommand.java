package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.messages.outgoing.rooms.RoomRelativeMapComposer;

public class UpdateYoutubePlaylistsCommand extends Command {
    public UpdateYoutubePlaylistsCommand() {
        super("cmd_update_youtube_playlists", Emulator.getTexts().getValue("commands.keys.cmd_update_youtube_playlists").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        Emulator.getGameEnvironment().getItemManager().getYoutubeManager().load();

        gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.succes.cmd_update_youtube_playlists"), RoomChatMessageBubbles.ALERT);

        return true;
    }
}

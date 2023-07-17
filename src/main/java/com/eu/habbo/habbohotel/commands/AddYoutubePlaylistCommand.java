package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.YoutubeManager;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Slf4j
public class AddYoutubePlaylistCommand extends Command {

    public AddYoutubePlaylistCommand() {
        super("cmd_add_youtube_playlist", Emulator.getTexts().getValue("commands.keys.cmd_add_youtube_playlist").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (params.length < 3) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_add_youtube_playlist.usage"));
            return true;
        }

        int itemId;

        try {
            itemId = Integer.parseInt(params[1]);
        } catch (NumberFormatException e) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_add_youtube_playlist.no_base_item"));
            return true;
        }

        if (Emulator.getGameEnvironment().getItemManager().getItem(itemId) == null) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_add_youtube_playlist.no_base_item"));
            return true;
        }

        YoutubeManager.YoutubePlaylist playlist = Emulator.getGameEnvironment().getItemManager().getYoutubeManager().getPlaylistDataById(params[2]);

        if (playlist == null) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_add_youtube_playlist.failed_playlist"));
            return true;
        }

        Emulator.getGameEnvironment().getItemManager().getYoutubeManager().addPlaylistToItem(Integer.parseInt(params[1]), playlist);

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT INTO `youtube_playlists` (`item_id`, `playlist_id`) VALUES (?, ?)")) {
            statement.setInt(1, itemId);
            statement.setString(2, params[2]);

            statement.execute();
        } catch (SQLException e) {
            log.error("Caught SQL exception", e);
        }

        gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.succes.cmd_add_youtube_playlist"));

        return true;
    }
}

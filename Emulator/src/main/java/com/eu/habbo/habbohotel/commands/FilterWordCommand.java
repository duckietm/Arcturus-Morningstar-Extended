package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.modtool.WordFilter;
import com.eu.habbo.habbohotel.modtool.WordFilterWord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class FilterWordCommand extends Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(FilterWordCommand.class);

    public FilterWordCommand() {
        super("cmd_filterword", Emulator.getTexts().getValue("commands.keys.cmd_filterword").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (params.length < 2) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_filterword.missing_word"));
            return true;
        }

        String word = params[1];

        String replacement = WordFilter.DEFAULT_REPLACEMENT;
        if (params.length == 3) {
            replacement = params[2];
        }

        WordFilterWord wordFilterWord = new WordFilterWord(word, replacement);

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT INTO wordfilter (`key`, `replacement`) VALUES (?, ?)")) {
            statement.setString(1, word);
            statement.setString(2, replacement);
            statement.execute();
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_filterword.error"));
            return true;
        }

        gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.succes.cmd_filterword.added").replace("%word%", word).replace("%replacement%", replacement));
        Emulator.getGameEnvironment().getWordFilter().addWord(wordFilterWord);

        return true;
    }
}
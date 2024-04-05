package com.eu.habbo.core.consolecommands;

import com.eu.habbo.Emulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShowRCONCommands extends ConsoleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShowRCONCommands.class);

    public ShowRCONCommands() {
        super("rconcommands", "Show a list of all RCON commands");
    }

    @Override
    public void handle(String[] args) throws Exception {
        for (String command : Emulator.getRconServer().getCommands()) {
            LOGGER.info(command);
        }
    }
}

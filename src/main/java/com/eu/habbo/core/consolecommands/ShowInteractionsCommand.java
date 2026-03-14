package com.eu.habbo.core.consolecommands;

import com.eu.habbo.Emulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShowInteractionsCommand extends ConsoleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShowInteractionsCommand.class);

    public ShowInteractionsCommand() {
        super("interactions", "Show a list of available furniture interactions.");
    }

    @Override
    public void handle(String[] args) throws Exception {
        for (String interaction : Emulator.getGameEnvironment().getItemManager().getInteractionList()) {
            LOGGER.info(interaction);
        }
    }
}
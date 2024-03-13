package com.eu.habbo.core.consolecommands;

import com.eu.habbo.Emulator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ShowRCONCommands extends ConsoleCommand {

    public ShowRCONCommands() {
        super("rconcommands", "Show a list of all RCON commands");
    }

    @Override
    public void handle(String[] args) throws Exception {
        for (String command : Emulator.getRconServer().getCommands()) {
            log.info(command);
        }
    }
}

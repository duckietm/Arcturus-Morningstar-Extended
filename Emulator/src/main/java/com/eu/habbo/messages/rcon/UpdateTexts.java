package com.eu.habbo.messages.rcon;

import com.eu.habbo.Emulator;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CUSTOM RCON key "updatetexts": reload emulator_texts and the command keys derived from them. */
public class UpdateTexts extends RCONMessage<UpdateTexts.JSONUpdateTexts> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateTexts.class);

    public UpdateTexts() {
        super(JSONUpdateTexts.class);
    }

    @Override
    public void handle(Gson gson, JSONUpdateTexts json) {
        try {
            Emulator.getTexts().reload();
            Emulator.getGameEnvironment().getCommandHandler().reloadCommands();
        } catch (Exception exception) {
            LOGGER.error("Texts reload failed", exception);
            this.status = STATUS_ERROR;
            this.message = "texts reload failed: " + exception.getMessage();
        }
    }

    static class JSONUpdateTexts {
    }
}

package com.eu.habbo.messages.rcon;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.CommandHandler;
import com.eu.habbo.habbohotel.users.Habbo;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecuteCommand extends RCONMessage<ExecuteCommand.JSONExecuteCommand> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteCommand.class);


    public ExecuteCommand() {
        super(JSONExecuteCommand.class);
    }

    @Override
    public void handle(Gson gson, JSONExecuteCommand json) {
        try {
            Habbo habbo = Emulator.getGameServer().getGameClientManager().getHabbo(json.user_id);

            if (habbo == null) {
                this.status = HABBO_NOT_FOUND;
                return;
            }


            CommandHandler.handleCommand(habbo.getClient(), json.command);
        } catch (Exception e) {
            this.status = STATUS_ERROR;
            LOGGER.error("Caught exception", e);
        }
    }

    static class JSONExecuteCommand {

        public int user_id;


        public String command;
    }
}
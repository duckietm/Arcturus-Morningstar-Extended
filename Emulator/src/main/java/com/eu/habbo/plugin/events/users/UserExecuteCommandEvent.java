package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.commands.Command;
import com.eu.habbo.habbohotel.users.Habbo;

public class UserExecuteCommandEvent extends UserEvent {

    public final Command command;
    public final String[] params;
    private boolean success;

    public UserExecuteCommandEvent(Habbo habbo, Command command, String[] params) {
        super(habbo);

        this.command = command;
        this.params = params;
        this.success = true;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }
}

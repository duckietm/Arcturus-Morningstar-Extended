package com.eu.habbo.messages.incoming.users;

import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.users.OnlineUsersComposer;

/** CUSTOM packet 10089: the ":online" window asks for a fresh list (refresh button / auto refresh). */
public class OnlineUsersRequestEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        if (this.client.getHabbo() == null) return;

        if (!this.client.getHabbo().hasPermission("cmd_online")
                && !OnlineUsersComposer.isStaff(this.client.getHabbo())) {
            return;
        }

        this.client.sendResponse(new OnlineUsersComposer(this.client.getHabbo()));
    }
}

package com.eu.habbo.messages.incoming.users;

import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.users.CustomWordFilterWordsComposer;

/** GetCustomFilter (145): sent when the word filter settings window opens. */
public class RequestCustomWordFilterEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        this.client.sendResponse(new CustomWordFilterWordsComposer(
                this.client.getHabbo().getHabboStats().getCustomWordFilter().words()));
    }
}

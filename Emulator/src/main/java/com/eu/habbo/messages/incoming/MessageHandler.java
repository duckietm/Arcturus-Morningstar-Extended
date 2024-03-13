package com.eu.habbo.messages.incoming;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.messages.ClientMessage;

public abstract class MessageHandler {
    public GameClient client;
    public ClientMessage packet;
    public boolean isCancelled = false;

    public abstract void handle() throws Exception;

    public int getRatelimit() {
        return 0;
    }
}
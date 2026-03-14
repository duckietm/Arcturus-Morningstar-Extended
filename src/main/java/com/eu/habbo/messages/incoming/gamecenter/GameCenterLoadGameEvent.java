package com.eu.habbo.messages.incoming.gamecenter;

import com.eu.habbo.messages.incoming.MessageHandler;

public class GameCenterLoadGameEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int gameId = this.packet.readInt();

        if (gameId == 3) {
        }
    }
}
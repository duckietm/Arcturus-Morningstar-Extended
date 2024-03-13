package com.eu.habbo.messages.incoming.gamecenter;

import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.gamecenter.GameCenterGameComposer;

public class GameCenterRequestGameStatusEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        this.client.sendResponse(new GameCenterGameComposer(this.packet.readInt(), GameCenterGameComposer.OK));
    }
}

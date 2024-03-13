package com.eu.habbo.messages.outgoing.gamecenter;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class GameCenterGameComposer extends MessageComposer {
    public final static int OK = 0;
    public final static int ERROR = 1;

    public final int gameId;
    public final int status;

    public GameCenterGameComposer(int gameId, int status) {
        this.gameId = gameId;
        this.status = status;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.GameCenterGameComposer);
        this.response.appendInt(this.gameId);
        this.response.appendInt(this.status);
        return this.response;
    }
}

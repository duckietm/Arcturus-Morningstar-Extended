package com.eu.habbo.messages.outgoing.camera;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class CameraCompetitionStatusComposer extends MessageComposer {
    private final boolean unknownBoolean;
    private final String unknownString;

    public CameraCompetitionStatusComposer(boolean unknownBoolean, String unknownString) {
        this.unknownBoolean = unknownBoolean;
        this.unknownString = unknownString;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.CameraCompetitionStatusComposer);
        this.response.appendBoolean(this.unknownBoolean);
        this.response.appendString(this.unknownString);
        return this.response;
    }
}
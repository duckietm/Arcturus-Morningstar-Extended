package com.eu.habbo.messages.outgoing.camera;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class CameraPublishWaitMessageComposer extends MessageComposer {
    public final boolean isOk;
    public final int cooldownSeconds;
    public final String extraDataId;

    public CameraPublishWaitMessageComposer(boolean isOk, int cooldownSeconds, String extraDataId) {
        this.isOk = isOk;
        this.cooldownSeconds = cooldownSeconds;
        this.extraDataId = extraDataId;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.CameraPublishWaitMessageComposer);

        this.response.appendBoolean(this.isOk);
        this.response.appendInt(this.cooldownSeconds);

        if (!this.extraDataId.isEmpty()) {
            this.response.appendString(this.extraDataId);
        }

        return this.response;
    }
}
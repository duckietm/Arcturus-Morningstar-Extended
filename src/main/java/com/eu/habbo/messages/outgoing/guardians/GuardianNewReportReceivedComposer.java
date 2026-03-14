package com.eu.habbo.messages.outgoing.guardians;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class GuardianNewReportReceivedComposer extends MessageComposer {
    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.GuardianNewReportReceivedComposer);
        this.response.appendInt(Emulator.getConfig().getInt("guardians.accept.timer"));
        return this.response;
    }
}

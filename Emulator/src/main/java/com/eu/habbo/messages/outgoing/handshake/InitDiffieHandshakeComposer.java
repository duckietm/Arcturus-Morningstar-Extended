package com.eu.habbo.messages.outgoing.handshake;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class InitDiffieHandshakeComposer extends MessageComposer {

    private final String signedPrime;
    private final String signedGenerator;

    public InitDiffieHandshakeComposer(String signedPrime, String signedGenerator) {
        this.signedPrime = signedPrime;
        this.signedGenerator = signedGenerator;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.InitDiffieHandshakeComposer);
        this.response.appendString(this.signedPrime);
        this.response.appendString(this.signedGenerator);
        return this.response;
    }

}

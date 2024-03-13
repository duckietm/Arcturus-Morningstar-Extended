package com.eu.habbo.messages.outgoing.handshake;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class CompleteDiffieHandshakeComposer extends MessageComposer {

    private final String publicKey;
    private final boolean clientEncryption;

    public CompleteDiffieHandshakeComposer(String publicKey) {
        this(publicKey, true);
    }

    public CompleteDiffieHandshakeComposer(String publicKey, boolean clientEncryption) {
        this.publicKey = publicKey;
        this.clientEncryption = clientEncryption;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.CompleteDiffieHandshakeComposer);
        this.response.appendString(this.publicKey);
        this.response.appendBoolean(this.clientEncryption);
        return this.response;
    }

}

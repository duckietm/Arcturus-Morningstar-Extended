package com.eu.habbo.messages.incoming.handshake;

import com.eu.habbo.messages.NoAuthMessage;
import com.eu.habbo.messages.incoming.MessageHandler;

@NoAuthMessage
public class MachineIDEvent extends MessageHandler {

    private static final int HASH_LENGTH = 64;

    @Override
    public void handle() throws Exception {
        String storedMachineId = this.packet.readString();
        String clientFingerprint = this.packet.readString();
        String capabilities = this.packet.readString();
        if (storedMachineId.length() > HASH_LENGTH) {
            storedMachineId = storedMachineId.substring(0, HASH_LENGTH);
        }
        this.client.setMachineId(storedMachineId);
    }
}

package com.eu.habbo.messages.incoming.handshake;

import com.eu.habbo.messages.NoAuthMessage;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.handshake.MachineIDComposer;
import com.eu.habbo.util.HexUtils;

@NoAuthMessage
public class MachineIDEvent extends MessageHandler {

    private static final int HASH_LENGTH = 64;

    @Override
    public void handle() throws Exception {
        String storedMachineId = this.packet.readString();
        String clientFingerprint = this.packet.readString();
        String capabilities = this.packet.readString();

        // Update stored machine id if it doesn't match our requirements.
        if (storedMachineId.startsWith("~") || storedMachineId.length() != HASH_LENGTH) {
            storedMachineId = HexUtils.getRandom(HASH_LENGTH);
            this.client.sendResponse(new MachineIDComposer(storedMachineId));
        }

        this.client.setMachineId(storedMachineId);
    }

}

package com.eu.habbo.messages.incoming.handshake;

import com.eu.habbo.messages.NoAuthMessage;
import com.eu.habbo.messages.incoming.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NoAuthMessage
public class MachineIDEvent extends MessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MachineIDEvent.class);

    private static final int HASH_LENGTH = 64;

    @Override
    public void handle() throws Exception {
        String storedMachineId = this.packet.readString();
        this.packet.readString();
        this.packet.readString();

        if (storedMachineId.length() > HASH_LENGTH) {
            storedMachineId = storedMachineId.substring(0, HASH_LENGTH);
        }

        this.client.setMachineId(storedMachineId);

        LOGGER.debug("Setting client MachineId to {}", storedMachineId);
    }
}
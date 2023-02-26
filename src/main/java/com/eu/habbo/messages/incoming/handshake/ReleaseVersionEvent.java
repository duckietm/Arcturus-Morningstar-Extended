package com.eu.habbo.messages.incoming.handshake;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.NoAuthMessage;
import com.eu.habbo.messages.incoming.MessageHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NoAuthMessage
public class ReleaseVersionEvent extends MessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecureLoginEvent.class);
	
	@Override
    public void handle() throws Exception {

        String releaseversionevent = this.packet.readString();

        if (releaseversionevent.isEmpty()) {
            Emulator.getGameServer().getGameClientManager().disposeClient(this.client);
            LOGGER.debug("Client is trying to connect without Release! Closed connection...");
            return;
        }
		LOGGER.debug(releaseversionevent);
        this.packet.readString();
    }
}

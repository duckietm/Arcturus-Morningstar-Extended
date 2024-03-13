package com.eu.habbo.messages.incoming.handshake;

import com.eu.habbo.messages.NoAuthMessage;
import com.eu.habbo.messages.incoming.MessageHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NoAuthMessage
public class ReleaseVersionEvent extends MessageHandler {
    public static boolean isReady = false;

    private static final Logger LOGGER = LoggerFactory.getLogger(SecureLoginEvent.class);
    
	@Override
    public void handle() throws Exception {

        // niet per se nodig
        this.client.finishedReleaseEvent();
    }
}

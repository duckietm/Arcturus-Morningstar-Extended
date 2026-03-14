package com.eu.habbo.networking.camera.messages.incoming;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.outgoing.generic.alerts.GenericAlertComposer;
import com.eu.habbo.networking.camera.CameraIncomingMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CameraUpdateNotification extends CameraIncomingMessage {

    private static final Logger LOGGER = LoggerFactory.getLogger(CameraUpdateNotification.class);

    public CameraUpdateNotification(Short header, ByteBuf body) {
        super(header, body);
    }

    @Override
    public void handle(Channel client) throws Exception {
        boolean alert = this.readBoolean();
        String message = this.readString();
        int type = this.readInt();

        if (type == 0) {
            LOGGER.info("Camera update: {}", message);
        } else if (type == 1) {
            LOGGER.warn("Camera update: {}", message);
        } else if (type == 2) {
            LOGGER.error("Camera update: {}", message);
        }

        if (alert) {
            Emulator.getGameServer().getGameClientManager().sendBroadcastResponse(new GenericAlertComposer(message).compose());
        }
    }
}
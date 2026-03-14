package com.eu.habbo.networking.camera;

import com.eu.habbo.networking.camera.messages.incoming.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class CameraPacketHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CameraPacketHandler.class);

    private static CameraPacketHandler INSTANCE;
    private final HashMap<Short, Class<? extends CameraIncomingMessage>> packetDefinitions;

    public CameraPacketHandler() {
        this.packetDefinitions = new HashMap<>();

        this.packetDefinitions.put((short) 1, CameraLoginStatusEvent.class);
        this.packetDefinitions.put((short) 2, CameraResultURLEvent.class);
        this.packetDefinitions.put((short) 3, CameraRoomThumbnailGeneratedEvent.class);
        this.packetDefinitions.put((short) 4, CameraUpdateNotification.class);
        this.packetDefinitions.put((short) 5, CameraAuthenticationTicketEvent.class);
    }

    public static CameraPacketHandler instance() {
        if (INSTANCE == null) {
            INSTANCE = new CameraPacketHandler();
        }

        return INSTANCE;
    }

    public void handle(Channel channel, short i, ByteBuf ii) {
        Class<? extends CameraIncomingMessage> declaredClass = this.packetDefinitions.get(i);

        if (declaredClass != null) {
            try {
                CameraIncomingMessage message = declaredClass.getDeclaredConstructor(new Class[]{Short.class, ByteBuf.class}).newInstance(i, ii);
                message.handle(channel);
                message.buffer.release();
            } catch (Exception e) {
                LOGGER.error("Caught exception", e);
            }
        }
    }
}
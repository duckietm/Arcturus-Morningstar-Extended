package com.eu.habbo.habbohotel.gameclients;

import com.eu.habbo.Emulator;
import com.eu.habbo.crypto.HabboEncryption;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.MessageComposer;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class GameClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameClient.class);

    private final Channel channel;
    private final HabboEncryption encryption;

    private Habbo habbo;
    private boolean handshakeFinished;
    private String machineId = "";

    public final ConcurrentHashMap<Integer, Integer> incomingPacketCounter = new ConcurrentHashMap<>(25);
    public final ConcurrentHashMap<Class<? extends MessageHandler>, Long> messageTimestamps = new ConcurrentHashMap<>();
    public long lastPacketCounterCleared = Emulator.getIntUnixTimestamp();

    public GameClient(Channel channel) {
        this.channel = channel;
        this.encryption = Emulator.getCrypto().isEnabled()
                ? new HabboEncryption(
                    Emulator.getCrypto().getExponent(),
                    Emulator.getCrypto().getModulus(),
                    Emulator.getCrypto().getPrivateExponent())
                : null;
    }

    public Channel getChannel() {
        return this.channel;
    }

    public HabboEncryption getEncryption() {
        return encryption;
    }

    public Habbo getHabbo() {
        return this.habbo;
    }

    public void setHabbo(Habbo habbo) {
        this.habbo = habbo;
    }

    public boolean isHandshakeFinished() {
        return handshakeFinished;
    }

    public void setHandshakeFinished(boolean handshakeFinished) {
        this.handshakeFinished = handshakeFinished;
    }

    public String getMachineId() {
        return this.machineId;
    }

    public void setMachineId(String machineId) {
        if (machineId == null) {
            throw new RuntimeException("Cannot set machineID to NULL");
        }

        this.machineId = machineId;
    }

    public void sendResponse(MessageComposer composer) {
        this.sendResponse(composer.compose());
    }

    public void sendResponse(ServerMessage response) {
        if (this.channel.isOpen()) {
            if (response == null || response.getHeader() <= 0) {
                return;
            }

            this.channel.write(response, this.channel.voidPromise());
            this.channel.flush();
        }
    }

    public void sendResponses(ArrayList<ServerMessage> responses) {
        if (this.channel.isOpen()) {
            for (ServerMessage response : responses) {
                if (response == null || response.getHeader() <= 0) {
                    return;
                }

                this.channel.write(response);
            }

            this.channel.flush();
        }
    }

    public void dispose() {
        try {
            this.channel.close();

            if (this.habbo != null) {
                if (this.habbo.isOnline()) {
                    this.habbo.getHabboInfo().setOnline(false);
                    this.habbo.disconnect();
                }

                this.habbo = null;
            }
        } catch (Exception e) {
            LOGGER.error("Caught exception", e);
        }
    }
}
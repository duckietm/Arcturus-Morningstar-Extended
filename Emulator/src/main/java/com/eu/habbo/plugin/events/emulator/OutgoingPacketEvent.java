package com.eu.habbo.plugin.events.emulator;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.plugin.events.users.UserEvent;

public class OutgoingPacketEvent extends UserEvent {
    private final MessageComposer composer;
    private final ServerMessage originalMessage;
    private ServerMessage customMessage;

    public OutgoingPacketEvent(Habbo habbo, MessageComposer composer, ServerMessage originalMessage) {
        super(habbo);
        this.composer = composer;
        this.originalMessage = originalMessage;
        this.customMessage = null;
    }

    public ServerMessage getOriginalMessage() {
        return originalMessage;
    }

    public MessageComposer getComposer() {
        return composer;
    }

    public void setCustomMessage(ServerMessage customMessage) {
        this.customMessage = customMessage;
    }

    public boolean hasCustomMessage() {
        return this.customMessage != null;
    }

    public ServerMessage getCustomMessage() {
        return this.customMessage;
    }
}
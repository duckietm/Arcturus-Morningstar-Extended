package com.eu.habbo.messages.incoming.users;

import com.eu.habbo.messages.incoming.MessageHandler;

public class ActivateEffectEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int effectId = this.packet.readInt();

        if (this.client.getHabbo().getInventory().getEffectsComponent().ownsEffect(effectId)) {
            this.client.getHabbo().getInventory().getEffectsComponent().activateEffect(effectId);
        }
    }
}
package com.eu.habbo.messages.outgoing.inventory;

import com.eu.habbo.habbohotel.users.inventory.EffectsComponent;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class EffectsListRemoveComposer extends MessageComposer {
    public final EffectsComponent.HabboEffect effect;

    public EffectsListRemoveComposer(EffectsComponent.HabboEffect effect) {
        this.effect = effect;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.EffectsListRemoveComposer);
        this.response.appendInt(this.effect.effect);
        return this.response;
    }
}
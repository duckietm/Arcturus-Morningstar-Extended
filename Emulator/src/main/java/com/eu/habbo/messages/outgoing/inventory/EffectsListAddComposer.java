package com.eu.habbo.messages.outgoing.inventory;

import com.eu.habbo.habbohotel.users.inventory.EffectsComponent;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class EffectsListAddComposer extends MessageComposer {
    public final EffectsComponent.HabboEffect effect;

    public
    EffectsListAddComposer(EffectsComponent.HabboEffect effect) {
        this.effect = effect;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.EffectsListAddComposer);
        this.response.appendInt(this.effect.effect); //Type
        this.response.appendInt(0); //Unknown Costume?
        this.response.appendInt(effect.duration > 0 ? effect.duration : Integer.MAX_VALUE); //Duration
        this.response.appendBoolean(effect.duration <= 0); //Is active
        return this.response;
    }
}
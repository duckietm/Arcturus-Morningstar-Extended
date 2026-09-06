package com.eu.habbo.messages.outgoing.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import it.unimi.dsi.fastutil.ints.Int2IntMap;

/** CUSTOM packet 10084: effect locks (special_enables: effect id -> minimum rank) for the effects window. */
public class EffectPoliciesComposer extends MessageComposer {
    private final boolean canEdit;
    private final Int2IntMap policies;

    public EffectPoliciesComposer(boolean canEdit) {
        this(canEdit, Emulator.getGameEnvironment().getPermissionsManager().getEffectRestrictions());
    }

    public EffectPoliciesComposer(boolean canEdit, Int2IntMap policies) {
        this.canEdit = canEdit;
        this.policies = policies;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.EffectPoliciesComposer);
        this.response.appendBoolean(this.canEdit);
        this.response.appendInt(this.policies.size());

        for (Int2IntMap.Entry entry : this.policies.int2IntEntrySet()) {
            this.response.appendInt(entry.getIntKey());
            this.response.appendInt(entry.getIntValue());
        }

        return this.response;
    }
}

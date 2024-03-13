package com.eu.habbo.messages.outgoing.rooms.users;

import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class RoomUserEffectComposer extends MessageComposer {
    private final RoomUnit roomUnit;
    private final int effectId;

    public RoomUserEffectComposer(RoomUnit roomUnit) {
        this.roomUnit = roomUnit;
        this.effectId = -1;
    }

    public RoomUserEffectComposer(RoomUnit roomUnit, int effectId) {
        this.roomUnit = roomUnit;
        this.effectId = effectId;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.RoomUserEffectComposer);
        this.response.appendInt(this.roomUnit.getId());
        this.response.appendInt(this.effectId == -1 ? this.roomUnit.getEffectId() : this.effectId);
        this.response.appendInt(0);
        return this.response;
    }
}

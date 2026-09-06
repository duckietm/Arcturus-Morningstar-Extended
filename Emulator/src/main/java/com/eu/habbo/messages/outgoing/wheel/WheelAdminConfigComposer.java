package com.eu.habbo.messages.outgoing.wheel;

import com.eu.habbo.habbohotel.wheel.WheelManager;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

/** Fortune wheel admin: hotel-wide configuration and counters (CUSTOM packet 9405). */
public class WheelAdminConfigComposer extends MessageComposer {
    private final WheelManager wheel;

    public WheelAdminConfigComposer(WheelManager wheel) {
        this.wheel = wheel;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.WheelAdminConfigComposer);
        this.response.appendInt(this.wheel.getFreeSpinsPerDay());
        this.response.appendInt(this.wheel.getSpinCost());
        this.response.appendInt(this.wheel.getSpinCostType());
        this.response.appendInt(this.wheel.getRecentWinsCount());
        this.response.appendInt(WheelManager.RECENT_KEEP);
        this.response.appendInt(this.wheel.getTotalWeight());
        return this.response;
    }
}

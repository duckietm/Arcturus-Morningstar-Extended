package com.eu.habbo.messages.incoming.wheel;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.wheel.WheelManager;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.wheel.WheelAdminConfigComposer;
import com.eu.habbo.messages.outgoing.wheel.WheelDataComposer;

/** Fortune wheel admin: save free spins per day, spin cost and its currency (CUSTOM packet 9307). */
public class WheelAdminSaveConfigEvent extends MessageHandler {
    public static final String PERMISSION_KEY = "acc_wheeladmin";

    @Override
    public int getRatelimit() {
        return 1000;
    }

    @Override
    public void handle() throws Exception {
        if (this.client.getHabbo() == null || !this.client.getHabbo().hasPermission(PERMISSION_KEY)) {
            return;
        }

        int freeSpinsPerDay = this.packet.readInt();
        int spinCost = this.packet.readInt();
        int spinCostType = this.packet.readInt();

        WheelManager wheel = Emulator.getGameEnvironment().getWheelManager();
        wheel.updateSettings(freeSpinsPerDay, spinCost, spinCostType);

        this.client.sendResponse(new WheelAdminConfigComposer(wheel));
        this.client.sendResponse(new WheelDataComposer(
                wheel.getUserState(this.client.getHabbo().getHabboInfo().getId()),
                wheel.getSpinCost(), wheel.getSpinCostType(), wheel.getPrizes()));
    }
}

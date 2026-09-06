package com.eu.habbo.messages.incoming.wheel;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wheel.WheelManager;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.wheel.WheelAdminConfigComposer;
import com.eu.habbo.messages.outgoing.wheel.WheelRecentWinsComposer;

/** Fortune wheel admin: wipe the "latest winners" list (CUSTOM packet 9306). */
public class WheelAdminClearWinsEvent extends MessageHandler {
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

        WheelManager wheel = Emulator.getGameEnvironment().getWheelManager();
        wheel.clearRecentWins();

        // Everyone with the wheel open sees the empty list right away.
        WheelRecentWinsComposer wins = new WheelRecentWinsComposer(wheel.getRecentWins(50));
        for (Habbo habbo : Emulator.getGameEnvironment().getHabboManager().getOnlineHabbos().values()) {
            if (habbo != null && habbo.getClient() != null) habbo.getClient().sendResponse(wins);
        }

        this.client.sendResponse(new WheelAdminConfigComposer(wheel));
    }
}

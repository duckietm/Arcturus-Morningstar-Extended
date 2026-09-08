package com.eu.habbo.messages.incoming.hotlooks;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.hotlooks.HotLooksManager;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.hotlooks.HotLooksComposer;

/** AIR 13 GetHotLooks: the avatar editor asks for up to {@code count} looks of the user's gender. */
public class GetHotLooksEvent extends MessageHandler {
    @Override
    public int getRatelimit() {
        return 1000;
    }

    @Override
    public void handle() throws Exception {
        int count = this.packet.readInt();

        if (this.client.getHabbo() == null) {
            return;
        }

        HotLooksManager manager = Emulator.getGameEnvironment().getHotLooksManager();
        String gender = this.client.getHabbo().getHabboInfo().getGender().name();
        this.client.sendResponse(new HotLooksComposer(manager.getHotLooks(gender, count)));
    }
}

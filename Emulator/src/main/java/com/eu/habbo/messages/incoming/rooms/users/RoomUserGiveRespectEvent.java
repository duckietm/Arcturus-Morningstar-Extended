package com.eu.habbo.messages.incoming.rooms.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.plugin.events.users.UserRespectedEvent;

public class RoomUserGiveRespectEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int userId = this.packet.readInt();

        if (this.client.getHabbo().getHabboStats().respectPointsToGive > 0) {
            Habbo target = this.client.getHabbo().getHabboInfo().getCurrentRoom().getHabbo(userId);

            if (Emulator.getPluginManager().isRegistered(UserRespectedEvent.class, false)) {
                if (Emulator.getPluginManager().fireEvent(new UserRespectedEvent(target, this.client.getHabbo())).isCancelled())
                    return;
            }

            this.client.getHabbo().respect(target);
        }
    }
}

package com.eu.habbo.messages.incoming.rooms.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.plugin.events.users.UserRespectedEvent;

public class RoomUserGiveRespectEvent extends MessageHandler {

    @Override
    public int getRatelimit() {
        return 250;
    }

    @Override
    public void handle() throws Exception {
        int userId = this.packet.readInt();

        if (userId == client.getHabbo().getHabboInfo().getId()) {
            return;
        }

        if (this.client.getHabbo().getHabboStats().respectPointsToGive > 0) {
            if (this.client.getHabbo().getHabboInfo().getCurrentRoom() == null) return;

            Habbo target = this.client.getHabbo().getHabboInfo().getCurrentRoom().getHabbo(userId);
            if (target == null) return;

            if (Emulator.getPluginManager().isRegistered(UserRespectedEvent.class, false)) {
                if (Emulator.getPluginManager().fireEvent(new UserRespectedEvent(target, this.client.getHabbo())).isCancelled())
                    return;
            }

            this.client.getHabbo().respect(target);
        }
    }
}

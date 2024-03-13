package com.eu.habbo.messages.incoming.friends;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.friends.RoomInviteComposer;

public class InviteFriendsEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        if (this.client.getHabbo().getHabboStats().allowTalk()) {
            int[] userIds = new int[this.packet.readInt()];

            for (int i = 0; i < userIds.length; i++) {
                userIds[i] = this.packet.readInt();
            }

            String message = this.packet.readString();

            message = Emulator.getGameEnvironment().getWordFilter().filter(message, this.client.getHabbo());

            for (int i : userIds) {
                if (i == 0)
                    continue;

                Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(i);

                if (habbo != null) {
                    if (!habbo.getHabboStats().blockRoomInvites) {
                        habbo.getClient().sendResponse(new RoomInviteComposer(this.client.getHabbo().getHabboInfo().getId(), message));
                    }
                }
            }
        }
    }
}

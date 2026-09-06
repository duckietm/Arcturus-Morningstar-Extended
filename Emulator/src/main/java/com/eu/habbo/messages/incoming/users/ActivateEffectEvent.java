package com.eu.habbo.messages.incoming.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.incoming.MessageHandler;

public class ActivateEffectEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int effectId = this.packet.readInt();
        Habbo habbo = this.client.getHabbo();
        if (habbo == null) return;

        // CUSTOM: staff locks (special_enables) apply to everyone below the rank, owned effects included.
        int rankId = habbo.getHabboInfo().getRank().getId();
        if (Emulator.getGameEnvironment().getPermissionsManager().isEffectBlocked(effectId, rankId)) {
            return;
        }

        if (habbo.getInventory().getEffectsComponent().ownsEffect(effectId)) {
            habbo.getInventory().getEffectsComponent().activateEffect(effectId);
            return;
        }

        Room room = habbo.getHabboInfo().getCurrentRoom();
        if (room == null || habbo.getHabboInfo().getRiding() != null) return;

        room.giveEffect(habbo, effectId, -1);
    }
}

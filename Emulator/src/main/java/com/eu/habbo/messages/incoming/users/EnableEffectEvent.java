package com.eu.habbo.messages.incoming.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.incoming.MessageHandler;

public class EnableEffectEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int effectId = this.packet.readInt();
        Habbo habbo = this.client.getHabbo();
        if (habbo == null) return;

        if (effectId > 0) {
            // CUSTOM: staff locks (special_enables) apply to owned effects as well.
            int rankId = habbo.getHabboInfo().getRank().getId();
            if (Emulator.getGameEnvironment().getPermissionsManager().isEffectBlocked(effectId, rankId)) return;

            if (habbo.getInventory().getEffectsComponent().ownsEffect(effectId)) {
                habbo.getInventory().getEffectsComponent().enableEffect(effectId);
            }
        } else {
            habbo.getInventory().getEffectsComponent().activatedEffect = 0;

            if (habbo.getHabboInfo().getCurrentRoom() != null) {
                habbo.getHabboInfo().getCurrentRoom().giveEffect(habbo.getRoomUnit(), 0, -1);
            }
        }
    }
}

package com.eu.habbo.messages.incoming.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.users.EffectPoliciesComposer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CUSTOM packet 10092: the effects window asks for the lock policies (mode 0) or staff sets one
 * (mode 1: effect id, minimum rank; 0 unlocks). Locks live in special_enables and apply live.
 */
public class EffectPolicyEvent extends MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(EffectPolicyEvent.class);
    private static final int MODE_LIST = 0;
    private static final int MODE_SET = 1;
    private static final int MAX_RANK = 999;

    @Override
    public void handle() throws Exception {
        Habbo habbo = this.client.getHabbo();
        if (habbo == null) return;

        int mode = this.packet.readInt();
        boolean canEdit = habbo.hasPermission(Permission.ACC_CATALOGFURNI);

        if (mode == MODE_SET && canEdit) {
            int effectId = this.packet.readInt();
            int minRank = Math.max(0, Math.min(MAX_RANK, this.packet.readInt()));

            if (effectId > 0 && Emulator.getGameEnvironment().getPermissionsManager().setEffectMinRank(effectId, minRank)) {
                LOGGER.info("Effect {} lock set to rank {} by {}", effectId, minRank, habbo.getHabboInfo().getUsername());

                for (Habbo online : Emulator.getGameEnvironment().getHabboManager().getOnlineHabbos().values()) {
                    if (online == null || online.getClient() == null) continue;

                    stripLockedEffect(online, effectId, minRank);
                    online.getClient().sendResponse(new EffectPoliciesComposer(online.hasPermission(Permission.ACC_CATALOGFURNI)));
                }

                return;
            }
        } else if (mode == MODE_SET) {
            this.packet.readInt();
            this.packet.readInt();
        }

        this.client.sendResponse(new EffectPoliciesComposer(canEdit));
    }

    /** Someone below the new minimum rank who is wearing the effect right now loses it immediately. */
    private static void stripLockedEffect(Habbo habbo, int effectId, int minRank) {
        if (minRank <= 0 || habbo.getHabboInfo() == null || habbo.getHabboInfo().getRank() == null) return;
        if (habbo.getHabboInfo().getRank().getId() >= minRank) return;

        Room room = habbo.getHabboInfo().getCurrentRoom();
        if (room == null || habbo.getRoomUnit() == null) return;
        if (habbo.getRoomUnit().getEffectId() != effectId) return;

        habbo.getInventory().getEffectsComponent().activatedEffect = 0;
        room.giveEffect(habbo, 0, -1);
    }
}

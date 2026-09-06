package com.eu.habbo.messages.incoming.users;

import com.eu.habbo.habbohotel.users.HabboBadge;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.users.UserBadgesComposer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UserWearBadgeEvent extends MessageHandler {
    @Override
    public int getRatelimit() {
        return 250;
    }

    @Override
    public void handle() throws Exception {
        Map<Integer, HabboBadge> requestedSlots = new HashMap<>();
        Set<String> usedCodes = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            int slot = this.packet.readInt();
            if (slot < 1 || slot > 5)
                return;

            String badgeId = this.packet.readString();

            if (badgeId.isEmpty())
                continue;

            HabboBadge badge = this.client.getHabbo().getInventory().getBadgesComponent().getBadge(badgeId);
            if (badge == null || !usedCodes.add(badge.getCode().toUpperCase()) || requestedSlots.containsKey(slot)) return;
            requestedSlots.put(slot, badge);
        }

        if (!this.client.getHabbo().getInventory().getBadgesComponent()
                .replaceWearingBadges(this.client.getHabbo(), requestedSlots)) return;

        var updatedBadges = this.client.getHabbo().getInventory().getBadgesComponent().getWearingBadges();

        if (this.client.getHabbo().getHabboInfo().getCurrentRoom() != null) {
            this.client.getHabbo().getHabboInfo().getCurrentRoom().sendComposer(new UserBadgesComposer(updatedBadges, this.client.getHabbo().getHabboInfo().getId()).compose());
        } else {
            this.client.sendResponse(new UserBadgesComposer(updatedBadges, this.client.getHabbo().getHabboInfo().getId()));
        }
    }
}

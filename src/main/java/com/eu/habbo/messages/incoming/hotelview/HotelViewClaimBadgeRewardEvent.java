package com.eu.habbo.messages.incoming.hotelview;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.HabboBadge;
import com.eu.habbo.habbohotel.users.inventory.BadgesComponent;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.users.AddUserBadgeComposer;

public class HotelViewClaimBadgeRewardEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        String request = this.packet.readString();

        if (Emulator.getConfig().getBoolean("hotelview.badgereward." + request + ".enabled")) {
            String badgeCode = Emulator.getConfig().getValue("hotelview.badgereward." + request + "badge");

            if (!badgeCode.isEmpty()) {
                if (!this.client.getHabbo().getInventory().getBadgesComponent().hasBadge(badgeCode)) {
                    HabboBadge badge = BadgesComponent.createBadge(badgeCode, this.client.getHabbo());
                    this.client.sendResponse(new AddUserBadgeComposer(badge));
                }
            }
        }
    }
}

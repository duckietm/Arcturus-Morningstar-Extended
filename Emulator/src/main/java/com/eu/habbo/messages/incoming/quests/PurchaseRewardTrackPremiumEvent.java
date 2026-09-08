package com.eu.habbo.messages.incoming.quests;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;

/** PurchaseRewardTrackPremium (3022): buy the premium tier of a reward track. */
public class PurchaseRewardTrackPremiumEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        String trackId = this.packet.readString();
        Emulator.getGameEnvironment().getRewardTrackManager().purchasePremium(this.client.getHabbo(), trackId);
    }
}

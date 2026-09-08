package com.eu.habbo.messages.incoming.quests;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;

/** StartCampaign (1697): the tracker auto-starts the default campaign. */
public class StartCampaignEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        String campaignCode = this.packet.readString();
        Emulator.getGameEnvironment().getQuestManager().startCampaign(this.client.getHabbo(), campaignCode);
    }
}

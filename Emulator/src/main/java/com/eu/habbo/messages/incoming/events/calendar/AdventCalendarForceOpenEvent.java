package com.eu.habbo.messages.incoming.events.calendar;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;

public class AdventCalendarForceOpenEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        String campaignName = this.packet.readString();
        int day = this.packet.readInt();

        Emulator.getGameEnvironment().getCalendarManager().claimCalendarReward(this.client.getHabbo(), campaignName, day, true);
    }
}
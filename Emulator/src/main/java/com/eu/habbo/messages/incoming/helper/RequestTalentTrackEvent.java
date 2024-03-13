package com.eu.habbo.messages.incoming.helper;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.achievements.TalentTrackType;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.achievements.talenttrack.TalentTrackComposer;

public class RequestTalentTrackEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        if (Emulator.getConfig().getBoolean("hotel.talenttrack.enabled")) {
            this.client.sendResponse(new TalentTrackComposer(this.client.getHabbo(), TalentTrackType.valueOf(this.packet.readString().toUpperCase())));
        }
    }
}

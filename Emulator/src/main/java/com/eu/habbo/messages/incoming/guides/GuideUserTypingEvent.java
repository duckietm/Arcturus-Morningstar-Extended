package com.eu.habbo.messages.incoming.guides;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.guides.GuideTour;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.guides.GuideSessionPartnerIsTypingComposer;

public class GuideUserTypingEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        boolean typing = this.packet.readBoolean();

        GuideTour tour = Emulator.getGameEnvironment().getGuideManager().getGuideTourByHabbo(this.client.getHabbo());

        if (tour != null) {
            if (tour.getHelper() == this.client.getHabbo()) {
                tour.getNoob().getClient().sendResponse(new GuideSessionPartnerIsTypingComposer(typing));
            } else {
                tour.getHelper().getClient().sendResponse(new GuideSessionPartnerIsTypingComposer(typing));
            }
        }
    }
}

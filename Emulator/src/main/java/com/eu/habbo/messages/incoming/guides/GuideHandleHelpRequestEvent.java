package com.eu.habbo.messages.incoming.guides;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.guides.GuideTour;
import com.eu.habbo.messages.incoming.MessageHandler;

public class GuideHandleHelpRequestEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        GuideTour tour = Emulator.getGameEnvironment().getGuideManager().getGuideTourByHelper(this.client.getHabbo());

        if (tour == null) {
            return;
        }

        boolean accepted = this.packet.readBoolean();

        if (!accepted) {
            Emulator.getGameEnvironment().getGuideManager().declineTour(tour);
        } else {
            Emulator.getGameEnvironment().getGuideManager().startSession(tour, this.client.getHabbo());
        }
    }
}

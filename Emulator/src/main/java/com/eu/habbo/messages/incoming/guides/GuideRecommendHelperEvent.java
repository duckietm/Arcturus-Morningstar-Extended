package com.eu.habbo.messages.incoming.guides;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.guides.GuideTour;
import com.eu.habbo.messages.incoming.MessageHandler;

public class GuideRecommendHelperEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        boolean recommend = this.packet.readBoolean();

        GuideTour tour = Emulator.getGameEnvironment().getGuideManager().getGuideTourByNoob(this.client.getHabbo());

        if (tour != null) {
            Emulator.getGameEnvironment().getGuideManager().recommend(tour, recommend);
        }
    }
}

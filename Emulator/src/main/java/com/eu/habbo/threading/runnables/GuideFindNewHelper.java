package com.eu.habbo.threading.runnables;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.guides.GuideTour;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.guides.GuideSessionDetachedComposer;

public class GuideFindNewHelper implements Runnable {
    private final GuideTour tour;
    private final Habbo helper;
    private final int checkSum;

    public GuideFindNewHelper(GuideTour tour, Habbo helper) {
        this.tour = tour;
        this.helper = helper;
        this.checkSum = tour.checkSum;
    }

    @Override
    public void run() {
        if (!this.tour.isEnded() && this.tour.checkSum == this.checkSum && this.tour.getHelper() == null) {
            if (this.helper != null && this.helper.getClient() != null) {
                this.helper.getClient().sendResponse(new GuideSessionDetachedComposer());
            }

            Emulator.getGameEnvironment().getGuideManager().findHelper(this.tour);
        }
    }
}
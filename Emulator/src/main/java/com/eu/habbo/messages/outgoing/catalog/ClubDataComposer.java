package com.eu.habbo.messages.outgoing.catalog;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.catalog.ClubOffer;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.subscriptions.Subscription;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.List;

public class ClubDataComposer extends MessageComposer {
    private final int windowId;
    private final Habbo habbo;

    public ClubDataComposer(Habbo habbo, int windowId) {
        this.habbo = habbo;
        this.windowId = windowId;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.ClubDataComposer);

        List<ClubOffer> offers = Emulator.getGameEnvironment().getCatalogManager().getClubOffers(this.windowId);
        this.response.appendInt(offers.size());

        for (ClubOffer offer : offers) {
            int expireTimestamp = offer.isBuildersClubSubscription()
                    ? this.habbo.getHabboStats().getSubscriptionExpireTimestamp(Subscription.BUILDERS_CLUB)
                    : (offer.isBuildersClubAddon() ? Emulator.getIntUnixTimestamp() : this.habbo.getHabboStats().getClubExpireTimestamp());

            offer.serialize(this.response, expireTimestamp);
        }

        this.response.appendInt(this.windowId);
        return this.response;
    }

    public int getWindowId() {
        return windowId;
    }

    public Habbo getHabbo() {
        return habbo;
    }
}

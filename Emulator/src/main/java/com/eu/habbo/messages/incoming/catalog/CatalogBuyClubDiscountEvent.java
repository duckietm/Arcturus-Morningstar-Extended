package com.eu.habbo.messages.incoming.catalog;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.catalog.ClubOffer;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.users.subscriptions.Subscription;
import com.eu.habbo.habbohotel.users.subscriptions.SubscriptionHabboClub;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.catalog.AlertPurchaseFailedComposer;
import com.eu.habbo.messages.outgoing.catalog.PurchaseOKComposer;
import com.eu.habbo.messages.outgoing.inventory.InventoryRefreshComposer;
import com.eu.habbo.messages.outgoing.unknown.ExtendClubMessageComposer;
import com.eu.habbo.messages.outgoing.users.UserClubComposer;
import com.eu.habbo.messages.outgoing.users.UserCreditsComposer;
import com.eu.habbo.messages.outgoing.users.UserCurrencyComposer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CatalogBuyClubDiscountEvent extends MessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogBuyClubDiscountEvent.class);

    @Override
    public void handle() throws Exception {

        Subscription subscription = this.client.getHabbo().getHabboStats().getSubscription(SubscriptionHabboClub.HABBO_CLUB);

        int days = 0;
        int minutes = 0;
        int timeRemaining = 0;

        if(subscription != null) {
            timeRemaining = subscription.getRemaining();
            days = (int) Math.floor(timeRemaining / 86400.0);
            minutes = (int) Math.ceil(timeRemaining / 60.0);

            if(days < 1 && minutes > 0) {
                days = 1;
            }
        }

        if(timeRemaining > 0 && SubscriptionHabboClub.DISCOUNT_ENABLED && days <= SubscriptionHabboClub.DISCOUNT_DAYS_BEFORE_END) {
            ClubOffer deal = Emulator.getGameEnvironment().getCatalogManager().clubOffers.values().stream().filter(ClubOffer::isDeal).findAny().orElse(null);

            if(deal != null) {
                ClubOffer regular = Emulator.getGameEnvironment().getCatalogManager().getClubOffers().stream().filter(x -> x.getDays() == deal.getDays()).findAny().orElse(null);
                if(regular != null) {

                    int totalDays = deal.getDays();
                    int totalCredits = deal.getCredits();
                    int totalDuckets = deal.getPoints();

                    if (totalDays > 0) {
                        if (this.client.getHabbo().getHabboInfo().getCurrencyAmount(deal.getPointsType()) < totalDuckets)
                            return;

                        if (this.client.getHabbo().getHabboInfo().getCredits() < totalCredits)
                            return;

                        if (!this.client.getHabbo().hasPermission(Permission.ACC_INFINITE_CREDITS))
                            this.client.getHabbo().giveCredits(-totalCredits);

                        if (!this.client.getHabbo().hasPermission(Permission.ACC_INFINITE_POINTS))
                            this.client.getHabbo().givePoints(deal.getPointsType(), -totalDuckets);


                        if(this.client.getHabbo().getHabboStats().createSubscription(Subscription.HABBO_CLUB, (totalDays * 86400)) == null) {
                            this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR).compose());
                            throw new Exception("Unable to create or extend subscription");
                        }

                        this.client.sendResponse(new PurchaseOKComposer(null));
                        this.client.sendResponse(new InventoryRefreshComposer());

                        this.client.getHabbo().getHabboStats().run();
                    }
                }
            }
        }

    }
}

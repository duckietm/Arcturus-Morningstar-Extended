package com.eu.habbo.messages.incoming.catalog;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.catalog.CatalogItem;
import com.eu.habbo.habbohotel.catalog.CatalogManager;
import com.eu.habbo.habbohotel.catalog.TargetOffer;
import com.eu.habbo.habbohotel.users.cache.HabboOfferPurchase;
import com.eu.habbo.messages.incoming.MessageHandler;

public class PurchaseTargetOfferEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        int offerId = this.packet.readInt();
        int amount = this.packet.readInt();

        if (amount <= 0) return;


        if (Emulator.getIntUnixTimestamp() - this.client.getHabbo().getHabboStats().lastPurchaseTimestamp >= CatalogManager.PURCHASE_COOLDOWN) {
            this.client.getHabbo().getHabboStats().lastPurchaseTimestamp = Emulator.getIntUnixTimestamp();

            TargetOffer offer = Emulator.getGameEnvironment().getCatalogManager().getTargetOffer(offerId);

            HabboOfferPurchase purchase = HabboOfferPurchase.getOrCreate(this.client.getHabbo(), offerId);

            if (purchase != null) {
                amount = Math.min(offer.getPurchaseLimit() - purchase.getAmount(), amount);
                int now = Emulator.getIntUnixTimestamp();
                if (offer.getExpirationTime() > now) {
                    purchase.update(amount, now);
                    CatalogItem item = Emulator.getGameEnvironment().getCatalogManager().getCatalogItem(offer.getCatalogItem());
                    if (item.isLimited()) {
                        amount = 1;
                    }
                    Emulator.getGameEnvironment().getCatalogManager().purchaseItem(null, item, this.client.getHabbo(), amount, "", false);

                }
            }
        }
    }
}
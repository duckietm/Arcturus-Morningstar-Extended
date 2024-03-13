package com.eu.habbo.messages.incoming.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.NewUserGift;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.plugin.events.users.UserPickGiftEvent;

public class PickNewUserGiftEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int totalItems = this.packet.readInt(); //total items
        int keyA = this.packet.readInt(); //key 1
        int keyB = this.packet.readInt(); //key 2
        int index = this.packet.readInt();

        if (!Emulator.getPluginManager().fireEvent(new UserPickGiftEvent(this.client.getHabbo(), keyA, keyB, index)).isCancelled()) {
            if (!this.client.getHabbo().getHabboStats().nuxReward && Emulator.getConfig().getBoolean("hotel.nux.gifts.enabled")){
                this.client.getHabbo().getHabboStats().nuxReward = true;
                NewUserGift gift = Emulator.getGameEnvironment().getItemManager().getNewUserGift(index + 1);

                if (gift != null) {
                    gift.give(this.client.getHabbo());
                }
            }
        }
    }
}

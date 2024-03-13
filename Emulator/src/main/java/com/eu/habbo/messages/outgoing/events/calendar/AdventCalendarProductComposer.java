package com.eu.habbo.messages.outgoing.events.calendar;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.campaign.calendar.CalendarManager;
import com.eu.habbo.habbohotel.campaign.calendar.CalendarRewardObject;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class AdventCalendarProductComposer extends MessageComposer {
    public final boolean visible;
    public final CalendarRewardObject rewardObject;
    public final Habbo habbo;

    public AdventCalendarProductComposer(boolean visible, CalendarRewardObject rewardObject, Habbo habbo) {
        this.visible = visible;
        this.rewardObject = rewardObject;
        this.habbo = habbo;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.AdventCalendarProductComposer);
        this.response.appendBoolean(this.visible);

        String className = "";
        String productName = this.rewardObject.getProductName()
                .replace("%credits%", String.valueOf(this.rewardObject.getCredits()))
                .replace("%pixels%", String.valueOf((int) (this.rewardObject.getPixels() * (habbo.getHabboStats().hasActiveClub() ? CalendarManager.HC_MODIFIER : 1.0))))
                .replace("%points%", String.valueOf(this.rewardObject.getPoints()))
                .replace("%points_type%", String.valueOf(this.rewardObject.getPointsType()))
                .replace("%badge%", this.rewardObject.getBadge());
        if(this.rewardObject.getSubscriptionType() != null){
            productName = productName.replace("%subscription_type%", this.rewardObject.getSubscriptionType()).replace("%subscription_days%", String.valueOf(this.rewardObject.getSubscriptionDays()));
        }

        if(this.rewardObject.getItem() != null){
            productName = productName.replace("%item%", this.rewardObject.getItem().getName());
            className = this.rewardObject.getItem().getName();
        }

        this.response.appendString(productName);
        this.response.appendString(this.rewardObject.getCustomImage());
        this.response.appendString(className);
        
        return this.response;
    }
}
package com.eu.habbo.plugin.events.users.calendar;

import com.eu.habbo.habbohotel.campaign.calendar.CalendarCampaign;
import com.eu.habbo.habbohotel.campaign.calendar.CalendarRewardObject;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.plugin.events.users.UserEvent;

public class UserClaimRewardEvent extends UserEvent {

    public CalendarCampaign campaign;
    public int day;
    public CalendarRewardObject reward;
    public boolean force;

    public UserClaimRewardEvent(Habbo habbo, CalendarCampaign campaign, int day, CalendarRewardObject reward, boolean force) {
        super(habbo);

        this.campaign = campaign;
        this.day = day;
        this.reward = reward;
        this.force = force;
    }
}

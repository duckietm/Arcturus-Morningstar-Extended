package com.eu.habbo.messages.outgoing.catalog;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.subscriptions.Subscription;
import com.eu.habbo.habbohotel.users.subscriptions.SubscriptionHabboClub;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class ClubCenterDataComposer extends MessageComposer {
    public final int currentHcStreak;
    public final String firstSubDate;
    public final double kickbackPercentage;
    public final int totalCreditsMissed;
    public final int totalCreditsRewarded;
    public final int totalCreditsSpent;
    public final int creditRewardForStreakBonus;
    public final int creditRewardForMonthlySpent;
    public final int timeUntilPayday;

    public ClubCenterDataComposer(int currentHcStreak, String firstSubDate, double kickbackPercentage, int totalCreditsMissed, int totalCreditsRewarded, int totalCreditsSpent, int creditRewardForStreakBonus, int creditRewardForMonthlySpent, int timeUntilPayday) {
        this.currentHcStreak = currentHcStreak;
        this.firstSubDate = firstSubDate;
        this.kickbackPercentage = kickbackPercentage;
        this.totalCreditsMissed = totalCreditsMissed;
        this.totalCreditsRewarded = totalCreditsRewarded;
        this.totalCreditsSpent = totalCreditsSpent;
        this.creditRewardForStreakBonus = creditRewardForStreakBonus;
        this.creditRewardForMonthlySpent = creditRewardForMonthlySpent;
        this.timeUntilPayday = timeUntilPayday;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.ClubCenterDataComposer);
        this.response.appendInt(this.currentHcStreak); // currentHcStreak (days)
        this.response.appendString(this.firstSubDate); // firstSubscriptionDate (dd-mm-yyyy)
        this.response.appendDouble(this.kickbackPercentage); // kickbackPercentage (e.g. 0.1 for 10%)
        this.response.appendInt(this.totalCreditsMissed); // (not used)
        this.response.appendInt(this.totalCreditsRewarded); // (not used)
        this.response.appendInt(this.totalCreditsSpent);
        this.response.appendInt(this.creditRewardForStreakBonus);
        this.response.appendInt(this.creditRewardForMonthlySpent);
        this.response.appendInt(this.timeUntilPayday); // timeUntilPayday (minutes)
        return this.response;
    }


}
package com.eu.habbo.habbohotel.users.subscriptions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.BuildersClubRoomSupport;
import com.eu.habbo.habbohotel.users.Habbo;

public class SubscriptionBuildersClub extends Subscription {
    public SubscriptionBuildersClub(Integer id, Integer userId, String subscriptionType, Integer timestampStart, Integer duration, Boolean active) {
        super(id, userId, subscriptionType, timestampStart, duration, active);
    }

    @Override
    public void onCreated() {
        super.onCreated();
        if (BuildersClubRoomSupport.syncOwnedRooms(this.getUserId()) > 0) {
            BuildersClubRoomSupport.sendRoomUnlockedBubble(this.getUserId());
        }
        BuildersClubRoomSupport.sendMembershipMadeBubble(this.getUserId());
        this.sendStatus();
    }

    @Override
    public void onExtended(int duration) {
        super.onExtended(duration);
        if (BuildersClubRoomSupport.syncOwnedRooms(this.getUserId()) > 0) {
            BuildersClubRoomSupport.sendRoomUnlockedBubble(this.getUserId());
        }
        BuildersClubRoomSupport.sendMembershipExtendedBubble(this.getUserId());
        this.sendStatus();
    }

    @Override
    public void onExpired() {
        super.onExpired();
        BuildersClubRoomSupport.syncOwnedRooms(this.getUserId());
        BuildersClubRoomSupport.sendMembershipExpiredAlert(
                this.getUserId(),
                BuildersClubRoomSupport.hasTrackedItemsInOwnedRooms(this.getUserId())
        );
        this.sendStatus();
    }

    private void sendStatus() {
        Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(this.getUserId());

        if (habbo != null && habbo.getClient() != null) {
            BuildersClubRoomSupport.sendPlacementStatus(habbo);
        }
    }
}

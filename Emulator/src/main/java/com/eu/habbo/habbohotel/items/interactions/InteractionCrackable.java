package com.eu.habbo.habbohotel.items.interactions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.achievements.AchievementManager;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.CrackableReward;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboGender;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.users.subscriptions.SubscriptionHabboClub;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.threading.runnables.CrackableExplode;
import com.eu.habbo.util.pathfinding.Rotation;

import java.sql.ResultSet;
import java.sql.SQLException;

public class InteractionCrackable extends HabboItem {
    private final Object lock = new Object();
    public boolean cracked = false;
    protected int ticks = 0;

    public InteractionCrackable(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public InteractionCrackable(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public void serializeExtradata(ServerMessage serverMessage) {
        int progress = this.readProgress();
        int target = Emulator.getGameEnvironment().getItemManager().getCrackableCount(this.getBaseItem().getId());

        serverMessage.appendInt(7 + (this.isLimited() ? 256 : 0));

        serverMessage.appendString(Emulator.getGameEnvironment().getItemManager().calculateCrackState(progress, target, this.getBaseItem()) + "");
        serverMessage.appendInt(progress);
        serverMessage.appendInt(target);

        super.serializeExtradata(serverMessage);
    }

    @Override
    public boolean canWalkOn(RoomUnit roomUnit, Room room, Object[] objects) {
        return true;
    }

    @Override
    public boolean isWalkable() {
        return false;
    }

    @Override
    public void onClick(GameClient client, Room room, Object[] objects) throws Exception {
        if (client == null) {
            return;
        }

        super.onClick(client, room, objects);
        synchronized (this.lock) {
            if (this.getRoomId() == 0)
                return;

            if (this.cracked)
                return;

            if (this.userRequiredToBeAdjacent() && client.getHabbo().getRoomUnit().getCurrentLocation().distance(room.getLayout().getTile(this.getX(), this.getY())) > 1.5) {
                client.getHabbo().getRoomUnit().setGoalLocation(room.getLayout().getTileInFront(room.getLayout().getTile(this.getX(), this.getY()), Rotation.Calculate(client.getHabbo().getRoomUnit().getX(), client.getHabbo().getRoomUnit().getY(), this.getX(), this.getY())));
                return;
            }

            this.readProgress();

            if (this.getBaseItem().getEffectF() > 0)
                if (client.getHabbo().getHabboInfo().getGender().equals(HabboGender.F) && this.getBaseItem().getEffectF() == client.getHabbo().getRoomUnit().getEffectId())
                    return;

            if (this.getBaseItem().getEffectM() > 0)
                if (client.getHabbo().getHabboInfo().getGender().equals(HabboGender.M) && this.getBaseItem().getEffectM() == client.getHabbo().getRoomUnit().getEffectId())
                    return;

            this.onTick(client.getHabbo(), room);
        }
    }

    public void onTick(Habbo habbo, Room room) {
        if (this.cracked) return;

        if (this.allowAnyone() || this.getUserId() == habbo.getHabboInfo().getId()) {
            CrackableReward rewardData = Emulator.getGameEnvironment().getItemManager().getCrackableData(this.getBaseItem().getId());

            if (rewardData != null) {
                if (rewardData.count <= 0)
                    return;

                if (rewardData.requiredEffect > 0 && habbo.getRoomUnit().getEffectId() != rewardData.requiredEffect)
                    return;

                if(this.ticks < 1)
                {
                    // If there are no ticks (for example because the room has been reloaded), check the current extradata of the item and update the ticks.
                    this.ticks = Math.min(this.readProgress(), Math.max(0, rewardData.count - 1));
                }
                this.ticks++;
                this.setExtradata("" + (this.ticks));
                this.needsUpdate(true);
                room.updateItem(this);

                if (!rewardData.achievementTick.isEmpty()) {
                    AchievementManager.progressAchievement(habbo, Emulator.getGameEnvironment().getAchievementManager().getAchievement(rewardData.achievementTick));
                }
                if (!this.cracked && this.ticks >= rewardData.count) {
                    this.cracked = true;
                    Emulator.getThreading().run(new CrackableExplode(room, this, habbo, !this.placeInRoom(), this.getX(), this.getY()), 1500);

                    if (!rewardData.achievementCracked.isEmpty()) {
                        AchievementManager.progressAchievement(habbo, Emulator.getGameEnvironment().getAchievementManager().getAchievement(rewardData.achievementCracked));
                    }

                    if (rewardData.subscriptionType != null && rewardData.subscriptionDuration > 0) {
                        // subscriptions are given immediately upon cracking
                        switch (rewardData.subscriptionType) {
                            case HABBO_CLUB:
                                habbo.getHabboStats().createSubscription(SubscriptionHabboClub.HABBO_CLUB, rewardData.subscriptionDuration * 86400);
                                break;
                            case BUILDERS_CLUB:
                                habbo.getHabboStats().createSubscription("BUILDERS_CLUB", rewardData.subscriptionDuration * 86400);
                                break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onWalk(RoomUnit roomUnit, Room room, Object[] objects) throws Exception {

    }

    @Override
    public void onWalkOn(RoomUnit client, Room room, Object[] objects) throws Exception {

    }

    @Override
    public void onWalkOff(RoomUnit client, Room room, Object[] objects) throws Exception {

    }

    public boolean allowAnyone() {
        return false;
    }

    protected boolean placeInRoom() {
        return true;
    }

    public boolean resetable() {
        return false;
    }

    public boolean userRequiredToBeAdjacent() {
        return true;
    }

    public void reset(Room room) {
        this.cracked = false;
        this.ticks = 0;
        this.setExtradata("0");
        room.updateItem(this);
    }

    private int readProgress() {
        int progress = 0;
        try {
            progress = Math.max(0, Integer.parseInt(this.getExtradata()));
        } catch (NumberFormatException ignored) {
            // Old imports sometimes left non-numeric extra data on crackables.
        }

        String normalized = Integer.toString(progress);
        if (!normalized.equals(this.getExtradata())) {
            this.setExtradata(normalized);
            this.needsUpdate(true);
        }

        return progress;
    }

    @Override
    public boolean isUsable() {
        return false;
    }
}

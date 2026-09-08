package com.eu.habbo.habbohotel.quests;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboBadge;
import com.eu.habbo.messages.outgoing.users.AddUserBadgeComposer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Hands out the currency and badge rewards of quests, daily tasks and reward-track prizes. */
public final class QuestRewards {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuestRewards.class);

    public static final String TYPE_CREDITS = "credits";
    public static final String TYPE_DUCKETS = "duckets";
    public static final String TYPE_DIAMONDS = "diamonds";
    public static final String TYPE_BADGE = "badge";

    public static final int DIAMONDS_POINT_TYPE = 5;

    private QuestRewards() {}

    /** The official rule: HC members earn double duckets from quests. */
    public static int ducketAmount(int amount, boolean hasClub) {
        return hasClub ? amount * 2 : amount;
    }

    /** activityPointType style rewards: -1 credits, otherwise the points type (0 duckets, 5 diamonds). */
    public static void grantActivityPoints(Habbo habbo, int activityPointType, int amount) {
        if (habbo == null || amount < 1) {
            return;
        }
        if (activityPointType == Quest.REWARD_CREDITS) {
            habbo.giveCredits(amount, "quests.reward");
            return;
        }
        int granted = activityPointType == Quest.REWARD_DUCKETS
                ? ducketAmount(amount, habbo.getHabboStats().hasActiveClub())
                : amount;
        habbo.givePoints(activityPointType, granted, "quests.reward");
    }

    /** String reward types used by the daily tasks and the reward track. */
    public static void grantTyped(Habbo habbo, String rewardType, String extra, int amount) {
        if (habbo == null || rewardType == null) {
            return;
        }
        switch (rewardType.trim().toLowerCase()) {
            case TYPE_CREDITS -> grantActivityPoints(habbo, Quest.REWARD_CREDITS, amount);
            case TYPE_DUCKETS -> grantActivityPoints(habbo, Quest.REWARD_DUCKETS, amount);
            case TYPE_DIAMONDS -> grantActivityPoints(habbo, DIAMONDS_POINT_TYPE, amount);
            case TYPE_BADGE -> grantBadge(habbo, extra);
            default -> LOGGER.warn("Unknown quest reward type {}", rewardType);
        }
    }

    /** Maps the string reward type to the client activityPointType, -2 when it is not a currency. */
    public static int activityPointTypeOf(String rewardType) {
        if (rewardType == null) {
            return -2;
        }
        return switch (rewardType.trim().toLowerCase()) {
            case TYPE_CREDITS -> Quest.REWARD_CREDITS;
            case TYPE_DUCKETS -> Quest.REWARD_DUCKETS;
            case TYPE_DIAMONDS -> DIAMONDS_POINT_TYPE;
            default -> -2;
        };
    }

    public static void grantBadge(Habbo habbo, String badgeCode) {
        if (habbo == null || badgeCode == null || badgeCode.isBlank()) {
            return;
        }
        if (habbo.getInventory().getBadgesComponent().hasBadge(badgeCode)) {
            return;
        }
        HabboBadge badge = new HabboBadge(0, badgeCode, 0, habbo);
        badge.run();
        habbo.getInventory().getBadgesComponent().addBadge(badge);
        if (habbo.getClient() != null) {
            habbo.getClient().sendResponse(new AddUserBadgeComposer(badge));
        }
    }
}

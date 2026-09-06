package com.eu.habbo.habbohotel.items;

import com.eu.habbo.Emulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class CrackableReward {
    private static final Logger LOGGER = LoggerFactory.getLogger(CrackableReward.class);

    public final int itemId;
    public final int count;
    public final Map<Integer, Map.Entry<Integer, Integer>> prizes;
    public final String achievementTick;
    public final String achievementCracked;
    public final int requiredEffect;
    public final int subscriptionDuration;
    public final RedeemableSubscriptionType subscriptionType;
    public int totalChance;

    public CrackableReward(ResultSet set) throws SQLException {
        this.itemId = set.getInt("item_id");
        this.count = set.getInt("count");
        this.achievementTick = set.getString("achievement_tick");
        this.achievementCracked = set.getString("achievement_cracked");
        this.requiredEffect = set.getInt("required_effect");
        this.subscriptionDuration = set.getInt("subscription_duration");
        this.subscriptionType = RedeemableSubscriptionType.fromString(set.getString("subscription_type"));


        String prizeList = set.getString("prizes");
        String[] prizes = prizeList == null ? new String[0] : prizeList.split(";");
        this.prizes = new LinkedHashMap<>();

        if (prizeList == null || prizeList.isBlank()) return;

        this.totalChance = 0;
        Map<Integer, Integer> weights = new LinkedHashMap<>();
        for (String rawPrize : prizes) {
            try {
                String prize = rawPrize.trim();
                if (prize.isEmpty()) continue;

                int itemId;
                int chance = 100;

                String[] parts = prize.split(":", -1);
                if (parts.length == 2) {
                    itemId = Integer.parseInt(parts[0].trim());
                    chance = Integer.parseInt(parts[1].trim());
                } else if (prize.contains(":")) {
                    LOGGER.error("Invalid configuration of crackable prizes (item id: {}). '{}' format should be itemId:chance.", this.itemId, prize);
                    continue;
                } else {
                    itemId = Integer.parseInt(prize);
                }

                if (itemId <= 0 || chance <= 0) {
                    LOGGER.error("Invalid non-positive crackable prize (item id: {}). '{}'.", this.itemId, prize);
                    continue;
                }

                weights.merge(itemId, chance, Integer::sum);
            } catch (Exception e) {
                LOGGER.error("Invalid configuration of crackable prizes (item id: {}). '{}'.", this.itemId, rawPrize, e);
            }
        }

        for (Map.Entry<Integer, Integer> reward : weights.entrySet()) {
            long candidate = (long) this.totalChance + reward.getValue();
            if (candidate > Integer.MAX_VALUE) {
                LOGGER.error("Crackable {} chance weights exceed the supported integer range.", this.itemId);
                break;
            }
            int upperBound = (int) candidate;
            this.prizes.put(reward.getKey(), new AbstractMap.SimpleEntry<>(this.totalChance, upperBound));
            this.totalChance = upperBound;
        }
    }

    public int getRandomReward() {
        if (this.prizes.isEmpty() || this.totalChance <= 0) return 0;

        int random = Emulator.getRandom().nextInt(this.totalChance);

        for (Map.Entry<Integer, Map.Entry<Integer, Integer>> set : this.prizes.entrySet()) {
            if (random >= set.getValue().getKey() && random < set.getValue().getValue()) {
                return set.getKey();
            }
        }

        return 0;
    }
}

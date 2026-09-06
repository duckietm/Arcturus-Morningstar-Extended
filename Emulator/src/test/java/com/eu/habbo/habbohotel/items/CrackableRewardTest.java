package com.eu.habbo.habbohotel.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import org.junit.jupiter.api.Test;

class CrackableRewardTest {
    @Test
    void mergesDuplicateRewardsAndRejectsMalformedOrNonPositiveWeights() throws Exception {
        ResultSet row = mock(ResultSet.class);
        when(row.getInt("item_id")).thenReturn(42);
        when(row.getInt("count")).thenReturn(2);
        when(row.getString("achievement_tick")).thenReturn("");
        when(row.getString("achievement_cracked")).thenReturn("");
        when(row.getString("subscription_type")).thenReturn(null);
        when(row.getString("prizes")).thenReturn("100:5;100:7;bad;200:3;300:0;:5");

        CrackableReward reward = new CrackableReward(row);

        assertEquals(2, reward.prizes.size());
        assertEquals(15, reward.totalChance);
        assertEquals(0, reward.prizes.get(100).getKey());
        assertEquals(12, reward.prizes.get(100).getValue());
        assertEquals(12, reward.prizes.get(200).getKey());
        assertEquals(15, reward.prizes.get(200).getValue());
    }

    @Test
    void allowsSubscriptionOnlyCrackablesWithoutFurniturePrizes() throws Exception {
        ResultSet row = mock(ResultSet.class);
        when(row.getInt("item_id")).thenReturn(43);
        when(row.getInt("count")).thenReturn(1);
        when(row.getString("achievement_tick")).thenReturn("");
        when(row.getString("achievement_cracked")).thenReturn("");
        when(row.getString("subscription_type")).thenReturn("hc");
        when(row.getString("prizes")).thenReturn("");

        CrackableReward reward = new CrackableReward(row);

        assertEquals(0, reward.prizes.size());
        assertEquals(0, reward.totalChance);
        assertEquals(RedeemableSubscriptionType.HABBO_CLUB, reward.subscriptionType);
    }
}

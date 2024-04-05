package com.eu.habbo.habbohotel.achievements;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.THashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TalentTrackLevel {
    private static final Logger LOGGER = LoggerFactory.getLogger(TalentTrackLevel.class);

    public TalentTrackType type;
    public int level;
    public TObjectIntMap<Achievement> achievements;
    public THashSet<Item> items;
    public String[] perks;
    public String[] badges;

    public TalentTrackLevel(ResultSet set) throws SQLException {
        this.type = TalentTrackType.valueOf(set.getString("type").toUpperCase());
        this.level = set.getInt("level");
        this.achievements = new TObjectIntHashMap<>();
        this.items = new THashSet<>();

        String[] achievements = set.getString("achievement_ids").split(",");
        String[] achievementLevels = set.getString("achievement_levels").split(",");
        if (achievementLevels.length == achievements.length) {
            for (int i = 0; i < achievements.length; i++) {
                if (achievements[i].isEmpty() || achievementLevels[i].isEmpty())
                    continue;

                Achievement achievement = Emulator.getGameEnvironment().getAchievementManager().getAchievement(Integer.valueOf(achievements[i]));

                if (achievement != null) {
                    this.achievements.put(achievement, Integer.valueOf(achievementLevels[i]));
                } else {
                    LOGGER.error("Could not find achievement with ID " + achievements[i] + " for talenttrack level " + this.level + " of type " + this.type);
                }
            }
        }

        for (String s : set.getString("reward_furni").split(",")) {
            Item item = Emulator.getGameEnvironment().getItemManager().getItem(Integer.valueOf(s));

            if (item != null) {
                this.items.add(item);
            } else {
                LOGGER.error("Incorrect reward furni (ID: " + s + ") for talent track level " + this.level);
            }
        }

        if (!set.getString("reward_perks").isEmpty()) {
            this.perks = set.getString("reward_perks").split(",");
        }

        if (!set.getString("reward_badges").isEmpty()) {
            this.badges = set.getString("reward_badges").split(",");
        }
    }
}
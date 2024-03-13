package com.eu.habbo.messages.outgoing.inventory;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.achievements.Achievement;
import com.eu.habbo.habbohotel.achievements.AchievementLevel;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import gnu.trove.map.hash.THashMap;
import gnu.trove.procedure.TObjectProcedure;

public class InventoryAchievementsComposer extends MessageComposer {
    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.InventoryAchievementsComposer);

        synchronized (Emulator.getGameEnvironment().getAchievementManager().getAchievements()) {
            THashMap<String, Achievement> achievements = Emulator.getGameEnvironment().getAchievementManager().getAchievements();

            this.response.appendInt(achievements.size());
            achievements.forEachValue(new TObjectProcedure<Achievement>() {
                @Override
                public boolean execute(Achievement achievement) {
                    InventoryAchievementsComposer.this.response.appendString((achievement.name.startsWith("ACH_") ? achievement.name.replace("ACH_", "") : achievement.name));
                    InventoryAchievementsComposer.this.response.appendInt(achievement.levels.size());

                    for (AchievementLevel level : achievement.levels.values()) {
                        InventoryAchievementsComposer.this.response.appendInt(level.level);
                        InventoryAchievementsComposer.this.response.appendInt(level.progress);
                    }

                    return true;
                }
            });
        }
        return this.response;
    }
}

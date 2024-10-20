package com.eu.habbo.habbohotel.wired;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.catalog.CatalogItem;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredExtra;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredTriggerReset;
import com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectGiveReward;
import com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTriggerStacks;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraRandom;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraUnseen;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboBadge;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.outgoing.catalog.PurchaseOKComposer;
import com.eu.habbo.messages.outgoing.inventory.AddHabboItemComposer;
import com.eu.habbo.messages.outgoing.inventory.InventoryRefreshComposer;
import com.eu.habbo.messages.outgoing.users.AddUserBadgeComposer;
import com.eu.habbo.messages.outgoing.wired.WiredRewardAlertComposer;
import com.eu.habbo.plugin.events.furniture.wired.WiredConditionFailedEvent;
import com.eu.habbo.plugin.events.furniture.wired.WiredStackExecutedEvent;
import com.eu.habbo.plugin.events.furniture.wired.WiredStackTriggeredEvent;
import com.eu.habbo.plugin.events.users.UserWiredRewardReceived;
import com.google.gson.GsonBuilder;
import gnu.trove.set.hash.THashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WiredHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(WiredHandler.class);

    //Configuration. Loaded from database & updated accordingly.
    public static int MAXIMUM_FURNI_SELECTION = 5;
    public static int TELEPORT_DELAY = 500;

    private static GsonBuilder gsonBuilder = null;

    public static boolean handle(WiredTriggerType triggerType, RoomUnit roomUnit, Room room, Object[] stuff) {
        if (triggerType == WiredTriggerType.CUSTOM) return false;

        boolean talked = false;

        if (!Emulator.isReady)
            return false;

        if (room == null)
            return false;

        if (!room.isLoaded())
            return false;

        if (room.getRoomSpecialTypes() == null)
            return false;

        THashSet<InteractionWiredTrigger> triggers = room.getRoomSpecialTypes().getTriggers(triggerType);

        if (triggers == null || triggers.isEmpty())
            return false;

        long millis = System.currentTimeMillis();
        THashSet<InteractionWiredEffect> effectsToExecute = new THashSet<InteractionWiredEffect>();

        List<RoomTile> triggeredTiles = new ArrayList<>();
        for (InteractionWiredTrigger trigger : triggers) {
            RoomTile tile = room.getLayout().getTile(trigger.getX(), trigger.getY());

            if (triggeredTiles.contains(tile))
                continue;

            THashSet<InteractionWiredEffect> tEffectsToExecute = new THashSet<InteractionWiredEffect>();

            if (handle(trigger, roomUnit, room, stuff, tEffectsToExecute)) {
                effectsToExecute.addAll(tEffectsToExecute);

                if (triggerType.equals(WiredTriggerType.SAY_SOMETHING))
                    talked = true;

                triggeredTiles.add(tile);
            }
        }

        for (InteractionWiredEffect effect : effectsToExecute) {
            triggerEffect(effect, roomUnit, room, stuff, millis);
        }

        return talked;
    }

    public static boolean handleCustomTrigger(Class<? extends InteractionWiredTrigger> triggerType, RoomUnit roomUnit, Room room, Object[] stuff) {
        if (!Emulator.isReady)
            return false;

        if (room == null)
            return false;

        if (!room.isLoaded())
            return false;

        if (room.getRoomSpecialTypes() == null)
            return false;

        THashSet<InteractionWiredTrigger> triggers = room.getRoomSpecialTypes().getTriggers(WiredTriggerType.CUSTOM);

        if (triggers == null || triggers.isEmpty())
            return false;

        long millis = System.currentTimeMillis();
        THashSet<InteractionWiredEffect> effectsToExecute = new THashSet<InteractionWiredEffect>();

        List<RoomTile> triggeredTiles = new ArrayList<>();
        for (InteractionWiredTrigger trigger : triggers) {
            if (trigger.getClass() != triggerType) continue;

            RoomTile tile = room.getLayout().getTile(trigger.getX(), trigger.getY());

            if (triggeredTiles.contains(tile))
                continue;

            THashSet<InteractionWiredEffect> tEffectsToExecute = new THashSet<InteractionWiredEffect>();

            if (handle(trigger, roomUnit, room, stuff, tEffectsToExecute)) {
                effectsToExecute.addAll(tEffectsToExecute);
                triggeredTiles.add(tile);
            }
        }

        for (InteractionWiredEffect effect : effectsToExecute) {
            triggerEffect(effect, roomUnit, room, stuff, millis);
        }

        return effectsToExecute.size() > 0;
    }

    public static boolean handle(InteractionWiredTrigger trigger, final RoomUnit roomUnit, final Room room, final Object[] stuff) {
        long millis = System.currentTimeMillis();
        THashSet<InteractionWiredEffect> effectsToExecute = new THashSet<InteractionWiredEffect>();

        if(handle(trigger, roomUnit, room, stuff, effectsToExecute)) {
            for (InteractionWiredEffect effect : effectsToExecute) {
                triggerEffect(effect, roomUnit, room, stuff, millis);
            }
            return true;
        }
        return false;
    }

    public static boolean handle(InteractionWiredTrigger trigger, final RoomUnit roomUnit, final Room room, final Object[] stuff, final THashSet<InteractionWiredEffect> effectsToExecute) {
        long millis = System.currentTimeMillis();
        int roomUnitId = roomUnit != null ? roomUnit.getId() : -1;
        if (Emulator.isReady && ((Emulator.getConfig().getBoolean("wired.custom.enabled", false) && (trigger.canExecute(millis) || roomUnitId > -1) && trigger.userCanExecute(roomUnitId, millis)) || (!Emulator.getConfig().getBoolean("wired.custom.enabled", false) && trigger.canExecute(millis))) && trigger.execute(roomUnit, room, stuff)) {
            trigger.activateBox(room, roomUnit, millis);

            THashSet<InteractionWiredCondition> conditions = room.getRoomSpecialTypes().getConditions(trigger.getX(), trigger.getY());
            THashSet<InteractionWiredEffect> effects = room.getRoomSpecialTypes().getEffects(trigger.getX(), trigger.getY());
            if (Emulator.getPluginManager().fireEvent(new WiredStackTriggeredEvent(room, roomUnit, trigger, effects, conditions)).isCancelled())
                return false;

            if (!conditions.isEmpty()) {
                ArrayList<WiredConditionType> matchedConditions = new ArrayList<>(conditions.size());
                for (InteractionWiredCondition searchMatched : conditions) {
                    if (!matchedConditions.contains(searchMatched.getType()) && searchMatched.operator() == WiredConditionOperator.OR && searchMatched.execute(roomUnit, room, stuff)) {
                        matchedConditions.add(searchMatched.getType());
                    }
                }

                for (InteractionWiredCondition condition : conditions) {
                    if (!((condition.operator() == WiredConditionOperator.OR && matchedConditions.contains(condition.getType())) ||
                            (condition.operator() == WiredConditionOperator.AND && condition.execute(roomUnit, room, stuff))) &&
                            !Emulator.getPluginManager().fireEvent(new WiredConditionFailedEvent(room, roomUnit, trigger, condition)).isCancelled()) {

                        return false;
                    }
                }
            }

            trigger.setCooldown(millis);

            boolean hasExtraRandom = room.getRoomSpecialTypes().hasExtraType(trigger.getX(), trigger.getY(), WiredExtraRandom.class);
            boolean hasExtraUnseen = room.getRoomSpecialTypes().hasExtraType(trigger.getX(), trigger.getY(), WiredExtraUnseen.class);
            THashSet<InteractionWiredExtra> extras = room.getRoomSpecialTypes().getExtras(trigger.getX(), trigger.getY());

            for (InteractionWiredExtra extra : extras) {
                extra.activateBox(room, roomUnit, millis);
            }

            List<InteractionWiredEffect> effectList = new ArrayList<>(effects);

            if (hasExtraRandom || hasExtraUnseen) {
                Collections.shuffle(effectList);
            }


            if (hasExtraUnseen) {
                for (InteractionWiredExtra extra : room.getRoomSpecialTypes().getExtras(trigger.getX(), trigger.getY())) {
                    if (extra instanceof WiredExtraUnseen) {
                        extra.setExtradata(extra.getExtradata().equals("1") ? "0" : "1");
                        InteractionWiredEffect effect = ((WiredExtraUnseen) extra).getUnseenEffect(effectList);
                        effectsToExecute.add(effect); // triggerEffect(effect, roomUnit, room, stuff, millis);
                        break;
                    }
                }
            } else {
                for (final InteractionWiredEffect effect : effectList) {
                    boolean executed = effectsToExecute.add(effect); //triggerEffect(effect, roomUnit, room, stuff, millis);
                    if (hasExtraRandom && executed) {
                        break;
                    }
                }
            }

            return !Emulator.getPluginManager().fireEvent(new WiredStackExecutedEvent(room, roomUnit, trigger, effects, conditions)).isCancelled();
        }

        return false;
    }

    private static boolean triggerEffect(InteractionWiredEffect effect, RoomUnit roomUnit, Room room, Object[] stuff, long millis) {
        boolean executed = false;
        if (effect != null && (effect.canExecute(millis) || (roomUnit != null && effect.requiresTriggeringUser() && Emulator.getConfig().getBoolean("wired.custom.enabled", false) && effect.userCanExecute(roomUnit.getId(), millis)))) {
            executed = true;
            if (!effect.requiresTriggeringUser() || (roomUnit != null && effect.requiresTriggeringUser())) {
                Emulator.getThreading().run(() -> {
                    if (room.isLoaded() && room.getHabbos().size() > 0) {
                        try {
                            if (!effect.execute(roomUnit, room, stuff)) return;
                            effect.setCooldown(millis);
                        } catch (Exception e) {
                            LOGGER.error("Caught exception", e);
                        }

                        effect.activateBox(room, roomUnit, millis);
                    }
                }, effect.getDelay() * 500);
            }
        }

        return executed;
    }

    public static GsonBuilder getGsonBuilder() {
        if(gsonBuilder == null) {
            gsonBuilder = new GsonBuilder();
        }
        return gsonBuilder;
    }

    public static boolean executeEffectsAtTiles(THashSet<RoomTile> tiles, final RoomUnit roomUnit, final Room room, final Object[] stuff) {
        for (RoomTile tile : tiles) {
            if (room != null) {
                THashSet<HabboItem> items = room.getItemsAt(tile);

                long millis = room.getCycleTimestamp();
                for (final HabboItem item : items) {
                    if (item instanceof InteractionWiredEffect && !(item instanceof WiredEffectTriggerStacks)) {
                        triggerEffect((InteractionWiredEffect) item, roomUnit, room, stuff, millis);
                        ((InteractionWiredEffect) item).setCooldown(millis);
                    }
                }
            }
        }

        return true;
    }

    public static void dropRewards(int wiredId) {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("DELETE FROM wired_rewards_given WHERE wired_item = ?")) {
            statement.setInt(1, wiredId);
            statement.execute();
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }
    }

    private static void giveReward(Habbo habbo, WiredEffectGiveReward wiredBox, WiredGiveRewardItem reward) {
        if (wiredBox.limit > 0)
            wiredBox.given++;

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT INTO wired_rewards_given (wired_item, user_id, reward_id, timestamp) VALUES ( ?, ?, ?, ?)")) {
            statement.setInt(1, wiredBox.getId());
            statement.setInt(2, habbo.getHabboInfo().getId());
            statement.setInt(3, reward.id);
            statement.setInt(4, Emulator.getIntUnixTimestamp());
            statement.execute();
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        if (reward.badge) {
            UserWiredRewardReceived rewardReceived = new UserWiredRewardReceived(habbo, wiredBox, "badge", reward.data);
            if (Emulator.getPluginManager().fireEvent(rewardReceived).isCancelled())
                return;

            if (rewardReceived.value.isEmpty())
                return;
            
            if (habbo.getInventory().getBadgesComponent().hasBadge(rewardReceived.value))
                return;

            HabboBadge badge = new HabboBadge(0, rewardReceived.value, 0, habbo);
            Emulator.getThreading().run(badge);
            habbo.getInventory().getBadgesComponent().addBadge(badge);
            habbo.getClient().sendResponse(new AddUserBadgeComposer(badge));
            habbo.getClient().sendResponse(new WiredRewardAlertComposer(WiredRewardAlertComposer.REWARD_RECEIVED_BADGE));
        } else {
            String[] data = reward.data.split("#");

            if (data.length == 2) {
                UserWiredRewardReceived rewardReceived = new UserWiredRewardReceived(habbo, wiredBox, data[0], data[1]);
                if (Emulator.getPluginManager().fireEvent(rewardReceived).isCancelled())
                    return;

                if (rewardReceived.value.isEmpty())
                    return;

                if (rewardReceived.type.equalsIgnoreCase("credits")) {
                    int credits = Integer.valueOf(rewardReceived.value);
                    habbo.giveCredits(credits);
                } else if (rewardReceived.type.equalsIgnoreCase("pixels")) {
                    int pixels = Integer.valueOf(rewardReceived.value);
                    habbo.givePixels(pixels);
                } else if (rewardReceived.type.startsWith("points")) {
                    int points = Integer.valueOf(rewardReceived.value);
                    int type = 5;

                    try {
                        type = Integer.valueOf(rewardReceived.type.replace("points", ""));
                    } catch (Exception e) {
                    }

                    habbo.givePoints(type, points);
                } else if (rewardReceived.type.equalsIgnoreCase("furni")) {
                    Item baseItem = Emulator.getGameEnvironment().getItemManager().getItem(Integer.valueOf(rewardReceived.value));
                    if (baseItem != null) {
                        HabboItem item = Emulator.getGameEnvironment().getItemManager().createItem(habbo.getHabboInfo().getId(), baseItem, 0, 0, "");

                        if (item != null) {
                            habbo.getClient().sendResponse(new AddHabboItemComposer(item));
                            habbo.getClient().getHabbo().getInventory().getItemsComponent().addItem(item);
                            habbo.getClient().sendResponse(new PurchaseOKComposer(null));
                            habbo.getClient().sendResponse(new InventoryRefreshComposer());
                            habbo.getClient().sendResponse(new WiredRewardAlertComposer(WiredRewardAlertComposer.REWARD_RECEIVED_ITEM));
                        }
                    }
                } else if (rewardReceived.type.equalsIgnoreCase("respect")) {
                    habbo.getHabboStats().respectPointsReceived += Integer.valueOf(rewardReceived.value);
                } else if (rewardReceived.type.equalsIgnoreCase("cata")) {
                    CatalogItem item = Emulator.getGameEnvironment().getCatalogManager().getCatalogItem(Integer.valueOf(rewardReceived.value));

                    if (item != null) {
                        Emulator.getGameEnvironment().getCatalogManager().purchaseItem(null, item, habbo, 1, "", true);
                    }
                    habbo.getClient().sendResponse(new WiredRewardAlertComposer(WiredRewardAlertComposer.REWARD_RECEIVED_ITEM));
                }
            }
        }
    }

    public static boolean getReward(Habbo habbo, WiredEffectGiveReward wiredBox) {
        if (wiredBox.limit > 0) {
            if (wiredBox.limit - wiredBox.given == 0) {
                habbo.getClient().sendResponse(new WiredRewardAlertComposer(WiredRewardAlertComposer.LIMITED_NO_MORE_AVAILABLE));
                return false;
            }
        }

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) as row_count, wired_rewards_given.* FROM wired_rewards_given WHERE user_id = ? AND wired_item = ? ORDER BY timestamp DESC LIMIT ?", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
            statement.setInt(1, habbo.getHabboInfo().getId());
            statement.setInt(2, wiredBox.getId());
            statement.setInt(3, wiredBox.rewardItems.size());

            try (ResultSet set = statement.executeQuery()) {
                if (set.first()) {
                    if (set.getInt("row_count") >= 1) {
                        if (wiredBox.rewardTime == WiredEffectGiveReward.LIMIT_ONCE) {
                            habbo.getClient().sendResponse(new WiredRewardAlertComposer(WiredRewardAlertComposer.REWARD_ALREADY_RECEIVED));
                            return false;
                        }
                    }

                    set.beforeFirst();
                    if (set.next()) {
                        if (wiredBox.rewardTime == WiredEffectGiveReward.LIMIT_N_MINUTES) {
                            if (Emulator.getIntUnixTimestamp() - set.getInt("timestamp") <= 60) {
                                habbo.getClient().sendResponse(new WiredRewardAlertComposer(WiredRewardAlertComposer.REWARD_ALREADY_RECEIVED_THIS_MINUTE));
                                return false;
                            }
                        }

                        if (wiredBox.uniqueRewards) {
                            if (set.getInt("row_count") == wiredBox.rewardItems.size()) {
                                habbo.getClient().sendResponse(new WiredRewardAlertComposer(WiredRewardAlertComposer.REWARD_ALL_COLLECTED));
                                return false;
                            }
                        }

                        if (wiredBox.rewardTime == WiredEffectGiveReward.LIMIT_N_HOURS) {
                            if (!(Emulator.getIntUnixTimestamp() - set.getInt("timestamp") >= (3600 * wiredBox.limitationInterval))) {
                                habbo.getClient().sendResponse(new WiredRewardAlertComposer(WiredRewardAlertComposer.REWARD_ALREADY_RECEIVED_THIS_HOUR));
                                return false;
                            }
                        }

                        if (wiredBox.rewardTime == WiredEffectGiveReward.LIMIT_N_DAY) {
                            if (!(Emulator.getIntUnixTimestamp() - set.getInt("timestamp") >= (86400 * wiredBox.limitationInterval))) {
                                habbo.getClient().sendResponse(new WiredRewardAlertComposer(WiredRewardAlertComposer.REWARD_ALREADY_RECEIVED_THIS_TODAY));
                                return false;
                            }
                        }
                    }

                    if (wiredBox.uniqueRewards) {
                        for (WiredGiveRewardItem item : wiredBox.rewardItems) {
                            set.beforeFirst();
                            boolean found = false;

                            while (set.next()) {
                                if (set.getInt("reward_id") == item.id)
                                    found = true;
                            }

                            if (!found) {
                                giveReward(habbo, wiredBox, item);
                                return true;
                            }
                        }
                    } else {
                        int randomNumber = Emulator.getRandom().nextInt(101);

                        int count = 0;
                        for (WiredGiveRewardItem item : wiredBox.rewardItems) {
                            if (randomNumber >= count && randomNumber <= (count + item.probability)) {
                                giveReward(habbo, wiredBox, item);
                                return true;
                            }

                            count += item.probability;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        return false;
    }

    public static void resetTimers(Room room) {
        if (!room.isLoaded() || room.getRoomSpecialTypes() == null)
            return;

        room.getRoomSpecialTypes().getTriggers().forEach(t -> {
            if (t == null) return;
            
            if (t.getType() == WiredTriggerType.AT_GIVEN_TIME || t.getType() == WiredTriggerType.PERIODICALLY || t.getType() == WiredTriggerType.PERIODICALLY_LONG) {
                ((WiredTriggerReset) t).resetTimer();
            }
        });

        room.setLastTimerReset(Emulator.getIntUnixTimestamp());
    }
}

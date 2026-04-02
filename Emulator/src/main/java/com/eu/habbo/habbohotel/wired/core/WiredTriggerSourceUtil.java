package com.eu.habbo.habbohotel.wired.core;

import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredExtra;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraFilterFurni;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraFilterFurniByVariable;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraFilterUser;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraFilterUsersByVariable;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import gnu.trove.set.hash.THashSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class WiredTriggerSourceUtil {
    private WiredTriggerSourceUtil() {
    }

    public static List<HabboItem> resolveItems(InteractionWiredTrigger trigger,
                                               WiredEvent event,
                                               int sourceType,
                                               Collection<HabboItem> selectedItems) {
        switch (sourceType) {
            case WiredSourceUtil.SOURCE_TRIGGER:
                return event.getSourceItem().map(Collections::singletonList).orElse(Collections.emptyList());
            case WiredSourceUtil.SOURCE_SELECTED:
                return (selectedItems != null) ? new ArrayList<>(selectedItems) : Collections.emptyList();
            case WiredSourceUtil.SOURCE_SELECTOR:
                return resolveSelectorItems(trigger, event);
            case WiredSourceUtil.SOURCE_SIGNAL:
                if (event.getType() == WiredEvent.Type.SIGNAL_RECEIVED) {
                    return event.getSourceItem().map(Collections::singletonList).orElse(Collections.emptyList());
                }
                return Collections.emptyList();
            default:
                return event.getSourceItem().map(Collections::singletonList).orElse(Collections.emptyList());
        }
    }

    public static List<RoomUnit> resolveUsers(InteractionWiredTrigger trigger,
                                              WiredEvent event,
                                              int sourceType,
                                              Collection<RoomUnit> selectedUsers) {
        switch (sourceType) {
            case WiredSourceUtil.SOURCE_TRIGGER:
                return event.getActor().map(Collections::singletonList).orElse(Collections.emptyList());
            case WiredSourceUtil.SOURCE_SELECTED:
                return (selectedUsers != null) ? new ArrayList<>(selectedUsers) : Collections.emptyList();
            case WiredSourceUtil.SOURCE_SELECTOR:
                return resolveSelectorUsers(trigger, event);
            case WiredSourceUtil.SOURCE_SIGNAL:
                if (event.getType() == WiredEvent.Type.SIGNAL_RECEIVED) {
                    return event.getActor().map(Collections::singletonList).orElse(Collections.emptyList());
                }
                return Collections.emptyList();
            default:
                return event.getActor().map(Collections::singletonList).orElse(Collections.emptyList());
        }
    }

    public static boolean containsItemOrTile(Room room, Collection<HabboItem> items, HabboItem sourceItem) {
        if (room == null || items == null || items.isEmpty() || sourceItem == null) {
            return false;
        }

        if (items.contains(sourceItem)) {
            return true;
        }

        for (HabboItem item : room.getItemsAt(sourceItem.getX(), sourceItem.getY())) {
            if (items.contains(item)) {
                return true;
            }
        }

        return false;
    }

    public static boolean containsUser(Collection<RoomUnit> users, RoomUnit target) {
        if (users == null || users.isEmpty() || target == null) {
            return false;
        }

        for (RoomUnit user : users) {
            if (user != null && user.getId() == target.getId()) {
                return true;
            }
        }

        return false;
    }

    private static List<HabboItem> resolveSelectorItems(InteractionWiredTrigger trigger, WiredEvent event) {
        WiredContext ctx = executeSelectors(trigger, event);

        if (ctx == null || !ctx.targets().isItemsModifiedBySelector()) {
            return Collections.emptyList();
        }

        return new ArrayList<>(ctx.targets().items());
    }

    private static List<RoomUnit> resolveSelectorUsers(InteractionWiredTrigger trigger, WiredEvent event) {
        WiredContext ctx = executeSelectors(trigger, event);

        if (ctx == null || !ctx.targets().isUsersModifiedBySelector()) {
            return Collections.emptyList();
        }

        return new ArrayList<>(ctx.targets().users());
    }

    private static WiredContext executeSelectors(InteractionWiredTrigger trigger, WiredEvent event) {
        if (trigger == null || event == null) {
            return null;
        }

        Room room = event.getRoom();
        if (room == null || room.getRoomSpecialTypes() == null) {
            return null;
        }

        WiredContext ctx = new WiredContext(event, trigger, DefaultWiredServices.getInstance(), new WiredState(100));

        for (InteractionWiredEffect effect : getOrderedSelectorEffects(room, trigger)) {
            if (effect.requiresActor() && !ctx.hasActor()) {
                continue;
            }

            try {
                ctx.state().step();
                effect.execute(ctx);
            } catch (Exception ignored) {
            }
        }

        applySelectionFilterExtras(room, trigger, ctx);

        return ctx;
    }

    private static List<InteractionWiredEffect> getOrderedSelectorEffects(Room room, InteractionWiredTrigger trigger) {
        if (room == null || trigger == null || room.getRoomSpecialTypes() == null) {
            return Collections.emptyList();
        }

        THashSet<InteractionWiredEffect> effects = room.getRoomSpecialTypes().getEffects(trigger.getX(), trigger.getY());
        List<InteractionWiredEffect> selectorEffects = new ArrayList<>();

        for (InteractionWiredEffect effect : WiredExecutionOrderUtil.sort(effects)) {
            if (effect != null && effect.isSelector()) {
                selectorEffects.add(effect);
            }
        }

        return selectorEffects;
    }

    private static void applySelectionFilterExtras(Room room, HabboItem triggerItem, WiredContext selectorCtx) {
        if (room == null || triggerItem == null || selectorCtx == null || room.getRoomSpecialTypes() == null) {
            return;
        }

        THashSet<InteractionWiredExtra> extras = room.getRoomSpecialTypes().getExtras(triggerItem.getX(), triggerItem.getY());

        if (extras == null || extras.isEmpty()) {
            return;
        }

        int furniLimit = Integer.MAX_VALUE;
        int userLimit = Integer.MAX_VALUE;
        List<WiredExtraFilterFurniByVariable> furniVariableFilters = new ArrayList<>();
        List<WiredExtraFilterUsersByVariable> userVariableFilters = new ArrayList<>();

        for (InteractionWiredExtra extra : extras) {
            if (extra instanceof WiredExtraFilterFurni) {
                furniLimit = Math.min(furniLimit, ((WiredExtraFilterFurni) extra).getAmount());
            } else if (extra instanceof WiredExtraFilterUser) {
                userLimit = Math.min(userLimit, ((WiredExtraFilterUser) extra).getAmount());
            } else if (extra instanceof WiredExtraFilterFurniByVariable) {
                furniVariableFilters.add((WiredExtraFilterFurniByVariable) extra);
            } else if (extra instanceof WiredExtraFilterUsersByVariable) {
                userVariableFilters.add((WiredExtraFilterUsersByVariable) extra);
            }
        }

        furniVariableFilters.sort((left, right) -> Integer.compare(left.getId(), right.getId()));
        userVariableFilters.sort((left, right) -> Integer.compare(left.getId(), right.getId()));

        if (selectorCtx.targets().isItemsModifiedBySelector()) {
            Iterable<HabboItem> filteredItems = selectorCtx.targets().items();

            for (WiredExtraFilterFurniByVariable extra : furniVariableFilters) {
                filteredItems = extra.filterItems(room, selectorCtx, filteredItems);
            }

            if (furniLimit != Integer.MAX_VALUE) {
                filteredItems = limitIterable(filteredItems, furniLimit);
            }

            selectorCtx.targets().setItems(filteredItems);
        }

        if (selectorCtx.targets().isUsersModifiedBySelector()) {
            Iterable<RoomUnit> filteredUsers = selectorCtx.targets().users();

            for (WiredExtraFilterUsersByVariable extra : userVariableFilters) {
                filteredUsers = extra.filterUsers(room, selectorCtx, filteredUsers);
            }

            if (userLimit != Integer.MAX_VALUE) {
                filteredUsers = limitIterable(filteredUsers, userLimit);
            }

            selectorCtx.targets().setUsers(filteredUsers);
        }
    }

    private static <T> List<T> limitIterable(Iterable<T> values, int limit) {
        List<T> result = new ArrayList<>();

        if (values == null || limit <= 0) {
            return result;
        }

        for (T value : values) {
            if (value != null) {
                result.add(value);
            }
        }

        if (result.size() <= limit) {
            return result;
        }

        return new ArrayList<>(result.subList(0, limit));
    }
}

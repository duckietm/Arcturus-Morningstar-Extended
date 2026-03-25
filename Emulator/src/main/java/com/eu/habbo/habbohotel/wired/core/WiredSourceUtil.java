package com.eu.habbo.habbohotel.wired.core;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredExtra;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraFilterFurni;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraFilterUser;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.api.IWiredEffect;
import gnu.trove.set.hash.THashSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class WiredSourceUtil {
    public static final int SOURCE_TRIGGER = 0;
    public static final int SOURCE_CLICKED_USER = 11;
    public static final int SOURCE_SELECTED = 100;
    public static final int SOURCE_SELECTOR = 200;
    public static final int SOURCE_SIGNAL = 201;

    private WiredSourceUtil() {
    }

    public static List<HabboItem> resolveItems(WiredContext ctx, int sourceType, Collection<HabboItem> selectedItems) {
        switch (sourceType) {
            case SOURCE_TRIGGER:
                return ctx.sourceItem().map(Collections::singletonList).orElse(Collections.emptyList());
            case SOURCE_SELECTED:
                return (selectedItems != null) ? new ArrayList<>(selectedItems) : Collections.emptyList();
            case SOURCE_SELECTOR:
                WiredTargets itemTargets = getSelectorTargets(ctx);
                return itemTargets.isItemsModifiedBySelector()
                        ? new ArrayList<>(itemTargets.items())
                        : Collections.emptyList();
            case SOURCE_SIGNAL:
                if (ctx.eventType() == WiredEvent.Type.SIGNAL_RECEIVED) {
                    return ctx.sourceItem().map(Collections::singletonList).orElse(Collections.emptyList());
                }
                return Collections.emptyList();
            default:
                return ctx.sourceItem().map(Collections::singletonList).orElse(Collections.emptyList());
        }
    }

    public static List<RoomUnit> resolveUsers(WiredContext ctx, int sourceType) {
        return resolveUsers(ctx, sourceType, null);
    }

    public static List<RoomUnit> resolveUsers(WiredContext ctx, int sourceType, Collection<RoomUnit> selectedUsers) {
        switch (sourceType) {
            case SOURCE_TRIGGER:
                return ctx.actor().map(Collections::singletonList).orElse(Collections.emptyList());
            case SOURCE_CLICKED_USER:
                if (ctx.eventType() == WiredEvent.Type.USER_CLICKS_USER) {
                    return ctx.event().getTargetUnit().map(Collections::singletonList).orElse(Collections.emptyList());
                }
                return Collections.emptyList();
            case SOURCE_SELECTED:
                return (selectedUsers != null) ? new ArrayList<>(selectedUsers) : Collections.emptyList();
            case SOURCE_SELECTOR:
                WiredTargets userTargets = getSelectorTargets(ctx);
                return userTargets.isUsersModifiedBySelector()
                        ? new ArrayList<>(userTargets.users())
                        : Collections.emptyList();
            case SOURCE_SIGNAL:
                if (ctx.eventType() == WiredEvent.Type.SIGNAL_RECEIVED) {
                    return ctx.actor().map(Collections::singletonList).orElse(Collections.emptyList());
                }
                return Collections.emptyList();
            default:
                return ctx.actor().map(Collections::singletonList).orElse(Collections.emptyList());
        }
    }

    public static boolean isDefaultUserSource(int value) {
        switch (value) {
            case SOURCE_TRIGGER:
            case SOURCE_CLICKED_USER:
            case SOURCE_SELECTOR:
            case SOURCE_SIGNAL:
                return true;
            default:
                return false;
        }
    }

    public static boolean isSelectableUserSource(int value) {
        return value == SOURCE_SELECTED || isDefaultUserSource(value);
    }

    private static WiredTargets getSelectorTargets(WiredContext ctx) {
        if (ctx == null) {
            return new WiredTargets();
        }

        if (ctx.targets().isItemsModifiedBySelector() || ctx.targets().isUsersModifiedBySelector()) {
            return ctx.targets();
        }

        WiredContext selectorContext = executeSelectors(ctx);

        if (selectorContext == null) {
            return ctx.targets();
        }

        if (selectorContext.targets().isItemsModifiedBySelector()) {
            ctx.targets().setItems(selectorContext.targets().items());
        }

        if (selectorContext.targets().isUsersModifiedBySelector()) {
            ctx.targets().setUsers(selectorContext.targets().users());
        }

        return ctx.targets();
    }

    private static WiredContext executeSelectors(WiredContext originalCtx) {
        if (originalCtx == null) {
            return null;
        }

        Room room = originalCtx.room();
        HabboItem triggerItem = originalCtx.triggerItem();

        if (room == null || triggerItem == null || room.getRoomSpecialTypes() == null) {
            return null;
        }

        WiredContext selectorCtx = new WiredContext(
                originalCtx.event(),
                triggerItem,
                originalCtx.stack(),
                originalCtx.services(),
                new WiredState(100),
                originalCtx.legacySettings()
        );

        List<InteractionWiredEffect> selectorEffects = getOrderedSelectorEffects(originalCtx, room, triggerItem);

        for (InteractionWiredEffect effect : selectorEffects) {
            if (effect.requiresActor() && !selectorCtx.hasActor()) {
                continue;
            }

            try {
                selectorCtx.state().step();
                effect.execute(selectorCtx);
            } catch (Exception ignored) {
            }
        }

        applySelectionFilterExtras(room, triggerItem, selectorCtx);

        return selectorCtx;
    }

    private static List<InteractionWiredEffect> getOrderedSelectorEffects(WiredContext originalCtx, Room room, HabboItem triggerItem) {
        List<InteractionWiredEffect> selectorEffects = new ArrayList<>();

        if (originalCtx != null && originalCtx.hasStack()) {
            for (IWiredEffect effect : originalCtx.stack().effects()) {
                if (effect instanceof InteractionWiredEffect && effect.isSelector()) {
                    selectorEffects.add((InteractionWiredEffect) effect);
                }
            }

            if (!selectorEffects.isEmpty()) {
                return selectorEffects;
            }
        }

        THashSet<InteractionWiredEffect> roomEffects = room.getRoomSpecialTypes().getEffects(triggerItem.getX(), triggerItem.getY());
        for (InteractionWiredEffect effect : WiredExecutionOrderUtil.sort(roomEffects)) {
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

        for (InteractionWiredExtra extra : extras) {
            if (extra instanceof WiredExtraFilterFurni) {
                furniLimit = Math.min(furniLimit, ((WiredExtraFilterFurni) extra).getAmount());
            } else if (extra instanceof WiredExtraFilterUser) {
                userLimit = Math.min(userLimit, ((WiredExtraFilterUser) extra).getAmount());
            }
        }

        if (selectorCtx.targets().isItemsModifiedBySelector() && furniLimit != Integer.MAX_VALUE) {
            selectorCtx.targets().setItems(limitIterable(selectorCtx.targets().items(), furniLimit));
        }

        if (selectorCtx.targets().isUsersModifiedBySelector() && userLimit != Integer.MAX_VALUE) {
            selectorCtx.targets().setUsers(limitIterable(selectorCtx.targets().users(), userLimit));
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

        Collections.shuffle(result, Emulator.getRandom());
        return new ArrayList<>(result.subList(0, limit));
    }
}

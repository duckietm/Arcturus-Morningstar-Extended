package com.eu.habbo.habbohotel.wired.core;

import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class WiredSourceUtil {
    public static final int SOURCE_TRIGGER = 0;
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
                return ctx.targets().isItemsModifiedBySelector()
                        ? new ArrayList<>(ctx.targets().items())
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
            case SOURCE_SELECTED:
                return (selectedUsers != null) ? new ArrayList<>(selectedUsers) : Collections.emptyList();
            case SOURCE_SELECTOR:
                return ctx.targets().isUsersModifiedBySelector()
                        ? new ArrayList<>(ctx.targets().users())
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
}

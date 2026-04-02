package com.eu.habbo.habbohotel.wired.core;

import com.eu.habbo.habbohotel.items.interactions.InteractionWiredExtra;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraVariableTextConnector;
import com.eu.habbo.habbohotel.rooms.Room;
import gnu.trove.set.hash.THashSet;

public final class WiredVariableTextConnectorSupport {
    private WiredVariableTextConnectorSupport() {
    }

    public static boolean isTextConnected(Room room, InteractionWiredExtra definition) {
        return getConnector(room, definition) != null;
    }

    public static boolean isTextConnected(Room room, int definitionItemId) {
        return getConnector(room, definitionItemId) != null;
    }

    public static WiredExtraVariableTextConnector getConnector(Room room, int definitionItemId) {
        if (room == null || room.getRoomSpecialTypes() == null || definitionItemId <= 0) {
            return null;
        }

        InteractionWiredExtra extra = room.getRoomSpecialTypes().getExtra(definitionItemId);
        return getConnector(room, extra);
    }

    public static WiredExtraVariableTextConnector getConnector(Room room, InteractionWiredExtra definition) {
        if (room == null || definition == null || room.getRoomSpecialTypes() == null) {
            return null;
        }

        THashSet<InteractionWiredExtra> extras = room.getRoomSpecialTypes().getExtras(definition.getX(), definition.getY());
        if (extras == null || extras.isEmpty()) {
            return null;
        }

        for (InteractionWiredExtra extra : WiredExecutionOrderUtil.sort(extras)) {
            if (extra instanceof WiredExtraVariableTextConnector) {
                return (WiredExtraVariableTextConnector) extra;
            }
        }

        return null;
    }

    public static String toText(Room room, int definitionItemId, Integer value) {
        if (value == null) {
            return "";
        }

        WiredExtraVariableTextConnector connector = getConnector(room, definitionItemId);
        return connector != null ? connector.resolveText(value) : String.valueOf(value);
    }

    public static Integer toValue(Room room, int definitionItemId, String text) {
        WiredExtraVariableTextConnector connector = getConnector(room, definitionItemId);
        return connector != null ? connector.resolveValue(text) : null;
    }
}

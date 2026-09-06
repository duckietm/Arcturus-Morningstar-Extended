package com.eu.habbo.habbohotel.wired;

import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraFurniVariable;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraUserVariable;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;

/**
 * Who may take a variable away from every holder at once. Unlike the other management actions this
 * one reaches users who are not in the room, so it is reserved for the owner of the definition box,
 * the way the official client reserves it, plus the staff permission that owns every room.
 */
public final class WiredVariableClearPolicy {

    private WiredVariableClearPolicy() {}

    public static boolean canClear(Room room, Habbo habbo, int definitionItemId) {
        if (room == null || habbo == null || habbo.getHabboInfo() == null) return false;
        if (!room.canModifyWired(habbo)) return false;

        HabboItem definition = room.getHabboItem(definitionItemId);
        if (!(definition instanceof WiredExtraUserVariable) && !(definition instanceof WiredExtraFurniVariable)) {
            return false;
        }

        return definition.getUserId() == habbo.getHabboInfo().getId()
                || habbo.hasPermission(Permission.ACC_ANYROOMOWNER);
    }
}

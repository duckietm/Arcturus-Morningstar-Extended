package com.eu.habbo.habbohotel.rooms;

import com.eu.habbo.habbohotel.items.interactions.InteractionWiredDisableControl;
import com.eu.habbo.habbohotel.users.HabboItem;

public final class RoomWiredDisableSupport {
    private static final String CONTROLLER_INTERACTION = "wf_conf_wired_disable";

    private RoomWiredDisableSupport() {
    }

    public static boolean isWiredDisabled(Room room) {
        if (room == null) {
            return false;
        }

        for (HabboItem item : room.getFloorItems()) {
            if (isActiveController(item)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isActiveController(HabboItem item) {
        return isControllerItem(item) && "1".equals(item.getExtradata());
    }

    public static boolean isControllerItem(HabboItem item) {
        if (item == null || item.getBaseItem() == null) {
            return false;
        }

        if (item instanceof InteractionWiredDisableControl) {
            return true;
        }

        if (item.getBaseItem().getInteractionType() == null) {
            return false;
        }

        String interactionName = item.getBaseItem().getInteractionType().getName();

        return interactionName != null && interactionName.equalsIgnoreCase(CONTROLLER_INTERACTION);
    }
}

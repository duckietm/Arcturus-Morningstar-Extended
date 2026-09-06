package com.eu.habbo.habbohotel.wired;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraFurniVariable;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraUserVariable;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.users.HabboItem;
import org.junit.jupiter.api.Test;

/**
 * Clearing a variable from every holder is the one management action that reaches people who are
 * not in the room, so it is reserved for the owner of the definition box, the way the official
 * client reserves it ("Only the owner of this variable can perform this action").
 */
class WiredVariableClearPolicyTest {

    private static final int OWNER_ID = 41, OTHER_ID = 42, DEFINITION_ID = 900;

    @Test
    void theOwnerOfTheDefinitionBoxMayClearIt() {
        Room room = roomWith(userDefinition(OWNER_ID), true);

        assertTrue(WiredVariableClearPolicy.canClear(room, habbo(OWNER_ID, false), DEFINITION_ID));
    }

    @Test
    void aFurniVariableBoxCountsToo() {
        Room room = roomWith(furniDefinition(OWNER_ID), true);

        assertTrue(WiredVariableClearPolicy.canClear(room, habbo(OWNER_ID, false), DEFINITION_ID));
    }

    @Test
    void somebodyElseWithWiredRightsMayNot() {
        Room room = roomWith(userDefinition(OWNER_ID), true);

        assertFalse(WiredVariableClearPolicy.canClear(room, habbo(OTHER_ID, false), DEFINITION_ID));
    }

    @Test
    void staffWhoOwnEveryRoomMay() {
        Room room = roomWith(userDefinition(OWNER_ID), true);

        assertTrue(WiredVariableClearPolicy.canClear(room, habbo(OTHER_ID, true), DEFINITION_ID));
    }

    @Test
    void theOwnerStillNeedsWiredRightsInTheRoom() {
        Room room = roomWith(userDefinition(OWNER_ID), false);

        assertFalse(WiredVariableClearPolicy.canClear(room, habbo(OWNER_ID, false), DEFINITION_ID));
    }

    @Test
    void aBoxThatIsNotAVariableDefinitionCannotBeCleared() {
        HabboItem notADefinition = mock(HabboItem.class);
        when(notADefinition.getUserId()).thenReturn(OWNER_ID);
        Room room = roomWith(notADefinition, true);

        assertFalse(WiredVariableClearPolicy.canClear(room, habbo(OWNER_ID, false), DEFINITION_ID));
    }

    @Test
    void aMissingBoxOrRoomOrHabboIsRefused() {
        Room room = roomWith(null, true);

        assertFalse(WiredVariableClearPolicy.canClear(room, habbo(OWNER_ID, false), DEFINITION_ID));
        assertFalse(WiredVariableClearPolicy.canClear(null, habbo(OWNER_ID, false), DEFINITION_ID));
        assertFalse(WiredVariableClearPolicy.canClear(room, null, DEFINITION_ID));
    }

    private static Room roomWith(HabboItem definition, boolean canModify) {
        Room room = mock(Room.class);
        when(room.getHabboItem(DEFINITION_ID)).thenReturn(definition);
        when(room.canModifyWired(org.mockito.ArgumentMatchers.any())).thenReturn(canModify);
        return room;
    }

    private static HabboItem userDefinition(int ownerId) {
        WiredExtraUserVariable definition = mock(WiredExtraUserVariable.class);
        when(definition.getUserId()).thenReturn(ownerId);
        return definition;
    }

    private static HabboItem furniDefinition(int ownerId) {
        WiredExtraFurniVariable definition = mock(WiredExtraFurniVariable.class);
        when(definition.getUserId()).thenReturn(ownerId);
        return definition;
    }

    private static Habbo habbo(int id, boolean ownsEveryRoom) {
        Habbo habbo = mock(Habbo.class);
        HabboInfo info = mock(HabboInfo.class);
        when(info.getId()).thenReturn(id);
        when(habbo.getHabboInfo()).thenReturn(info);
        when(habbo.hasPermission(Permission.ACC_ANYROOMOWNER)).thenReturn(ownsEveryRoom);
        return habbo;
    }
}

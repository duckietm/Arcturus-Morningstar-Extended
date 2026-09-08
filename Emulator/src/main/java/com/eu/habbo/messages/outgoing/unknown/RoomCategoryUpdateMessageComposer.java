package com.eu.habbo.messages.outgoing.unknown;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

/**
 * RoomCategorySelectionEnforcement (3896): tells the client the room it just
 * created or saved has no usable category, so it opens the modal enforce
 * category dialog (AIR EnforceCategoryCtrl.show(selectionType)) that answers
 * with UpdateRoomCategoryAndTradeSettings (1265).
 */
public class RoomCategoryUpdateMessageComposer extends MessageComposer {
    /** The dialog follows a room creation. */
    public static final int SELECTION_ROOM_CREATED = 1;

    /** The dialog follows a room settings save. */
    public static final int SELECTION_ROOM_SETTINGS = 2;

    private final int unknownInt1;

    public RoomCategoryUpdateMessageComposer(int unknownInt1) {
        this.unknownInt1 = unknownInt1;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.RoomCategoryUpdateMessageComposer);
        this.response.appendInt(this.unknownInt1);
        return this.response;
    }

    public int getUnknownInt1() {
        return unknownInt1;
    }
}

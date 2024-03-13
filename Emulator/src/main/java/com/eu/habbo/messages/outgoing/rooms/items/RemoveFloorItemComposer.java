package com.eu.habbo.messages.outgoing.rooms.items;

import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class RemoveFloorItemComposer extends MessageComposer {
    private final HabboItem item;
    private final boolean noUser;

    public RemoveFloorItemComposer(HabboItem item) {
        this.item = item;
        this.noUser = false;
    }

    public RemoveFloorItemComposer(HabboItem item, boolean noUser) {
        this.item = item;
        this.noUser = noUser;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.RemoveFloorItemComposer);

        this.response.appendString(this.item.getId() + "");
        this.response.appendBoolean(false);
        this.response.appendInt(this.noUser ? 0 : this.item.getUserId());
        this.response.appendInt(0);

        return this.response;
    }
}

package com.eu.habbo.messages.outgoing.rooms.users;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class RoomUserTagsComposer extends MessageComposer {
    private final Habbo habbo;

    public RoomUserTagsComposer(Habbo habbo) {
        this.habbo = habbo;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.RoomUserTagsComposer);
        this.response.appendInt(this.habbo.getRoomUnit().getId());
        this.response.appendInt(this.habbo.getHabboStats().tags.length);

        for (String tag : this.habbo.getHabboStats().tags) {
            this.response.appendString(tag);
        }
        return this.response;
    }
}

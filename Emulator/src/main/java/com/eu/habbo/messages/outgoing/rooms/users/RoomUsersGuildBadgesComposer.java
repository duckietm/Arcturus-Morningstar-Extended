package com.eu.habbo.messages.outgoing.rooms.users;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import gnu.trove.map.hash.THashMap;
import gnu.trove.procedure.TObjectObjectProcedure;

public class RoomUsersGuildBadgesComposer extends MessageComposer {
    private final THashMap<Integer, String> guildBadges;

    public RoomUsersGuildBadgesComposer(THashMap<Integer, String> guildBadges) {
        this.guildBadges = guildBadges;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.RoomUsersGuildBadgesComposer);
        this.response.appendInt(this.guildBadges.size());

        this.guildBadges.forEachEntry(new TObjectObjectProcedure<Integer, String>() {
            @Override
            public boolean execute(Integer guildId, String badge) {
                RoomUsersGuildBadgesComposer.this.response.appendInt(guildId);
                RoomUsersGuildBadgesComposer.this.response.appendString(badge);
                return true;
            }
        });
        return this.response;
    }
}
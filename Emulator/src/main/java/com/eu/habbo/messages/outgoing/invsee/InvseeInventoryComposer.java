package com.eu.habbo.messages.outgoing.invsee;

import com.eu.habbo.habbohotel.commands.invsee.InvseeService;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.List;

/** Staff inventory inspection: the target's hand grouped by base item. */
public class InvseeInventoryComposer extends MessageComposer {
    private final int targetUserId;
    private final String targetUsername;
    private final boolean online;
    private final List<InvseeService.ItemGroup> groups;

    public InvseeInventoryComposer(int targetUserId, String targetUsername, boolean online,
                                   List<InvseeService.ItemGroup> groups) {
        this.targetUserId = targetUserId;
        this.targetUsername = targetUsername;
        this.online = online;
        this.groups = groups;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.InvseeInventoryComposer);
        this.response.appendInt(this.targetUserId);
        this.response.appendString(this.targetUsername);
        this.response.appendBoolean(this.online);
        this.response.appendInt(this.groups.size());
        for (InvseeService.ItemGroup group : this.groups) {
            this.response.appendInt(group.baseItemId());
            this.response.appendInt(group.spriteId());
            this.response.appendString(group.name());
            this.response.appendInt(group.count());
        }
        return this.response;
    }
}

package com.eu.habbo.messages.outgoing.unknown;

import com.eu.habbo.messages.ISerialize;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class ModToolSanctionDataComposer extends MessageComposer {
    private final int unknownInt1;
    private final int accountId;
    private final CFHSanction sanction;

    public ModToolSanctionDataComposer(int unknownInt1, int accountId, CFHSanction sanction) {
        this.unknownInt1 = unknownInt1;
        this.accountId = accountId;
        this.sanction = sanction;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.ModToolSanctionDataComposer);
        this.response.appendInt(this.unknownInt1);
        this.response.appendInt(this.accountId);
        this.sanction.serialize(this.response);
        return this.response;
    }

    public static class CFHSanction implements ISerialize {
        private final String name;
        private final int length;
        private final int unknownInt1;
        private final boolean avatarOnly;
        private final String tradelockInfo;
        private final String machineBanInfo;

        public CFHSanction(String name, int length, int unknownInt1, boolean avatarOnly, String tradelockInfo, String machineBanInfo) {
            this.name = name;
            this.length = length;
            this.unknownInt1 = unknownInt1;
            this.avatarOnly = avatarOnly;
            this.tradelockInfo = tradelockInfo;
            this.machineBanInfo = machineBanInfo;
        }

        @Override
        public void serialize(ServerMessage message) {
            message.appendString(this.name);
            message.appendInt(this.length);
            message.appendInt(this.unknownInt1);
            message.appendBoolean(this.avatarOnly);
            message.appendString(this.tradelockInfo);
            message.appendString(this.machineBanInfo);
        }
    }
}
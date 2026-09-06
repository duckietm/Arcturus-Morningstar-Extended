package com.eu.habbo.messages.outgoing.housekeeping;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.modtool.ModToolBan;
import com.eu.habbo.habbohotel.permissions.Rank;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class HousekeepingUserDetailComposer extends MessageComposer {
    private static final int CURRENCY_DUCKETS = 0;
    private static final int CURRENCY_DIAMONDS = 5;

    private final HabboInfo info;

    public HousekeepingUserDetailComposer(HabboInfo info) {
        this.info = info;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.HousekeepingUserDetailComposer);

        if (this.info == null) {
            this.response.appendBoolean(false);
            return this.response;
        }

        Rank rank = this.info.getRank();
        ModToolBan ban = Emulator.getGameEnvironment().getModToolManager().checkForBan(this.info.getId());

        this.response.appendBoolean(true);
        this.response.appendInt(this.info.getId());
        this.response.appendString(safe(this.info.getUsername()));
        this.response.appendString(safe(this.info.getMotto()));
        this.response.appendString(safe(this.info.getLook()));
        this.response.appendInt(rank != null ? rank.getId() : 0);
        this.response.appendString(rank != null ? safe(rank.getName()) : "");
        this.response.appendBoolean(this.info.isOnline());
        this.response.appendInt(this.info.getLastOnline());
        // This legacy housekeeping packet has a signed-int field. Preserve its
        // layout while the authoritative wallet and USER_CREDITS packet remain
        // fully 64 bit.
        this.response.appendInt((int) Math.min(Integer.MAX_VALUE, this.info.getCreditsLong()));
        this.response.appendInt(this.info.getCurrencyAmount(CURRENCY_DUCKETS));
        this.response.appendInt(this.info.getCurrencyAmount(CURRENCY_DIAMONDS));
        this.response.appendString(safe(this.info.getMail()));
        this.response.appendString(safe(this.info.getIpLogin()));
        this.response.appendBoolean(ban != null);
        // Mute / trade-lock surface as future packet extensions; see the
        // optional-trailing-field parser pattern on the renderer side.
        this.response.appendBoolean(false);
        this.response.appendBoolean(false);

        return this.response;
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }
}

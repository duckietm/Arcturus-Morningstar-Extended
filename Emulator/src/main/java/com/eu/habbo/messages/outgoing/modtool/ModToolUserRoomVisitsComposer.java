package com.eu.habbo.messages.outgoing.modtool;

import com.eu.habbo.habbohotel.modtool.ModToolRoomVisit;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import gnu.trove.set.hash.THashSet;

import java.util.Calendar;
import java.util.TimeZone;

public class ModToolUserRoomVisitsComposer extends MessageComposer {
    private final HabboInfo habboInfo;
    private final THashSet<ModToolRoomVisit> roomVisits;

    public ModToolUserRoomVisitsComposer(HabboInfo habboInfo, THashSet<ModToolRoomVisit> roomVisits) {
        this.habboInfo = habboInfo;
        this.roomVisits = roomVisits;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.ModToolUserRoomVisitsComposer);
        this.response.appendInt(this.habboInfo.getId());
        this.response.appendString(this.habboInfo.getUsername());
        this.response.appendInt(this.roomVisits.size());

        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        for (ModToolRoomVisit visit : this.roomVisits) {
            cal.setTimeInMillis(visit.timestamp * 1000);
            this.response.appendInt(visit.roomId);
            this.response.appendString(visit.roomName);
            this.response.appendInt(cal.get(Calendar.HOUR));
            this.response.appendInt(cal.get(Calendar.MINUTE));
        }
        return this.response;
    }
}

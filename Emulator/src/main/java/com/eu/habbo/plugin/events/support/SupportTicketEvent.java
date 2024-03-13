package com.eu.habbo.plugin.events.support;

import com.eu.habbo.habbohotel.modtool.ModToolIssue;
import com.eu.habbo.habbohotel.users.Habbo;

public class SupportTicketEvent extends SupportEvent {
    public final ModToolIssue ticket;


    public SupportTicketEvent(Habbo moderator, ModToolIssue ticket) {
        super(moderator);

        this.ticket = ticket;
    }
}

package com.eu.habbo.messages.outgoing.events.resolution;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class NewYearResolutionComposer extends MessageComposer {
    @Override
    protected ServerMessage composeInternal() {
        //:test 817 i:230 i:1 i:1 i:1 s:NY2013RES i:3 i:0 i:60000000
        this.response.init(Outgoing.NewYearResolutionComposer);

        this.response.appendInt(230); //reward ID or item id? (stuffId)
        this.response.appendInt(2); //count

        this.response.appendInt(1); //achievementId
        this.response.appendInt(1); //achievementLevel
        this.response.appendString("NY2013RES");
        this.response.appendInt(3); //type ?
        this.response.appendInt(0); //Finished/ enabled

        this.response.appendInt(2); //achievementId
        this.response.appendInt(1); //achievementLevel
        this.response.appendString("ADM");
        this.response.appendInt(2); //type ?
        this.response.appendInt(0); //Finished/ enabled

        this.response.appendInt(1000); //Time in secs left.

        return this.response;
    }
}

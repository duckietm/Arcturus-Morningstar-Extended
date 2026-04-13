package com.eu.habbo.messages.incoming.catalog;

import com.eu.habbo.habbohotel.rooms.BuildersClubRoomSupport;
import com.eu.habbo.messages.incoming.MessageHandler;

public class BuildersClubQueryFurniCountEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        BuildersClubRoomSupport.sendPlacementStatus(this.client.getHabbo());
    }
}

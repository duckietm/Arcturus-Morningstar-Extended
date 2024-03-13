package com.eu.habbo.messages.incoming.navigator;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.RoomCategory;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.navigator.RoomCategoriesComposer;

import java.util.List;

public class RequestRoomCategoriesEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        List<RoomCategory> roomCategoryList = Emulator.getGameEnvironment().getRoomManager().roomCategoriesForHabbo(this.client.getHabbo());
        this.client.sendResponse(new RoomCategoriesComposer(roomCategoryList));
        //this.client.sendResponse(new NewNavigatorEventCategoriesComposer());
    }
}

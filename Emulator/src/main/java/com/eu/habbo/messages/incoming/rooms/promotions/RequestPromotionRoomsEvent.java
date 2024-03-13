package com.eu.habbo.messages.incoming.rooms.promotions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.rooms.promotions.PromoteOwnRoomsListComposer;

import java.util.List;

public class RequestPromotionRoomsEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        List<Room> roomsToShow = Emulator.getGameEnvironment().getRoomManager().getRoomsForHabbo(this.client.getHabbo());
        roomsToShow.addAll(Emulator.getGameEnvironment().getRoomManager().getRoomsWithRights(this.client.getHabbo()));
        roomsToShow.addAll(Emulator.getGameEnvironment().getRoomManager().getRoomsWithAdminRights(this.client.getHabbo()));
        this.client.sendResponse(new PromoteOwnRoomsListComposer(roomsToShow));
    }
}

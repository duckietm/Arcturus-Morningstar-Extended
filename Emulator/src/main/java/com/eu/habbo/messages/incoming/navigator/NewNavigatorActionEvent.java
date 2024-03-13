package com.eu.habbo.messages.incoming.navigator;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.rooms.ForwardToRoomComposer;
import com.eu.habbo.messages.outgoing.users.UserHomeRoomComposer;

import java.util.ArrayList;
import java.util.Collections;

public class NewNavigatorActionEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        String data = this.packet.readString();

        if (data.equals("random_friending_room")) {
            ArrayList<Room> rooms = Emulator.getGameEnvironment().getRoomManager().getActiveRooms();
            if (!rooms.isEmpty()) {
                Collections.shuffle(rooms);
                this.client.sendResponse(new ForwardToRoomComposer(rooms.get(0).getId()));
            }
        } else if (data.equalsIgnoreCase("predefined_noob_lobby")) {
            this.client.sendResponse(new ForwardToRoomComposer(Emulator.getConfig().getInt("hotel.room.nooblobby")));
        } else {
            this.client.sendResponse(new UserHomeRoomComposer(this.client.getHabbo().getHabboInfo().getHomeRoom(), this.client.getHabbo().getHabboInfo().getHomeRoom()));
        }
    }
}

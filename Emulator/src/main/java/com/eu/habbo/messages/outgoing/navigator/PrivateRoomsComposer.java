package com.eu.habbo.messages.outgoing.navigator;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PrivateRoomsComposer extends MessageComposer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrivateRoomsComposer.class);

    private final List<Room> rooms;

    public PrivateRoomsComposer(List<Room> rooms) {
        this.rooms = rooms;
    }

    @Override
    protected ServerMessage composeInternal() {
        try {
            this.response.init(Outgoing.PrivateRoomsComposer);

            this.response.appendInt(2);
            this.response.appendString("");

            this.response.appendInt(this.rooms.size());

            for (Room room : this.rooms) {
                room.serialize(this.response);
            }
            this.response.appendBoolean(true);

            this.response.appendInt(0);
            this.response.appendString("A");
            this.response.appendString("B");
            this.response.appendInt(1);
            this.response.appendString("C");
            this.response.appendString("D");
            this.response.appendInt(1);
            this.response.appendInt(1);
            this.response.appendInt(1);
            this.response.appendString("E");
            return this.response;
        } catch (Exception e) {
            LOGGER.error("Caught exception", e);
        }
        return null;
    }
}

package com.eu.habbo.messages.outgoing.rooms.promotions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomPromotion;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class RoomPromotionMessageComposer extends MessageComposer {
    private final Room room;
    private final RoomPromotion roomPromotion;

    public RoomPromotionMessageComposer(Room room, RoomPromotion roomPromotion) {
        this.room = room;
        this.roomPromotion = roomPromotion;
    }

    @Override
    protected ServerMessage composeInternal() {

        this.response.init(Outgoing.RoomEventMessageComposer);

        if (this.room == null || this.roomPromotion == null) {
            this.response.appendInt(-1);
            this.response.appendInt(-1);
            this.response.appendString("");
            this.response.appendInt(0);
            this.response.appendInt(0);
            this.response.appendString("");
            this.response.appendString("");
            this.response.appendInt(0);
            this.response.appendInt(0);
            this.response.appendInt(0);
        } else {
            this.response.appendInt(this.room.getId()); // promotion id
            this.response.appendInt(this.room.getOwnerId());
            this.response.appendString(this.room.getOwnerName());

            this.response.appendInt(this.room.getId()); // room id
            this.response.appendInt(1); // "type"

            this.response.appendString(this.roomPromotion.getTitle());
            this.response.appendString(this.roomPromotion.getDescription());
            this.response.appendInt((Emulator.getIntUnixTimestamp() - this.roomPromotion.getStartTimestamp()) / 60); // minutes since starting
            this.response.appendInt((this.roomPromotion.getEndTimestamp() - Emulator.getIntUnixTimestamp()) / 60); // minutes until end
            this.response.appendInt(this.roomPromotion.getCategory()); // category
        }

        return this.response;

    }

    public Room getRoom() {
        return room;
    }

    public RoomPromotion getRoomPromotion() {
        return roomPromotion;
    }
}

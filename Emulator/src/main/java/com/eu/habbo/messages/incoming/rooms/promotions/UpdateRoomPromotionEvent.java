package com.eu.habbo.messages.incoming.rooms.promotions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomPromotion;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.rooms.promotions.RoomPromotionMessageComposer;


public class UpdateRoomPromotionEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {

        int id = this.packet.readInt();
        String promotionName = this.packet.readString();
        String promotionDescription = this.packet.readString();

        Room room = Emulator.getGameEnvironment().getRoomManager().loadRoom(id);

        if (room == null || room.getOwnerId() != this.client.getHabbo().getHabboInfo().getId() || !this.client.getHabbo().hasPermission(Permission.ACC_ANYROOMOWNER)) {
            return;
        }

        RoomPromotion roomPromotion = room.getPromotion();

        if (roomPromotion != null) {

            roomPromotion.setTitle(promotionName);
            roomPromotion.setDescription(promotionDescription);

            roomPromotion.needsUpdate = true;
            roomPromotion.save();

            room.sendComposer(new RoomPromotionMessageComposer(room, roomPromotion).compose());
        }
    }
}

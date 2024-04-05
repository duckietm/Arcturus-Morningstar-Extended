package com.eu.habbo.messages.incoming.rooms.items;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;
import com.eu.habbo.messages.incoming.MessageHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClickFurnitureEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        try {
            final Room room = this.client.getHabbo().getRoomUnit().getRoom();

            if (room == null)
                return;

            int itemId = this.packet.readInt();

            HabboItem item = room.getHabboItem(itemId);

            if (item == null)
                return;

            WiredHandler.handle(WiredTriggerType.CLICK_FURNI, this.client.getHabbo().getRoomUnit(), room, new Object[]{item});
        } catch (Exception e) {
            LOGGER.error("Caught exception", e);
        }
    }
}

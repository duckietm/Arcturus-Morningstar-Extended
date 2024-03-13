package com.eu.habbo.messages.incoming.rooms.items;

import com.eu.habbo.habbohotel.items.interactions.InteractionRandomState;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.incoming.MessageHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UseRandomStateItemEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        try {
            int itemId = this.packet.readInt();
            int state = this.packet.readInt();

            Room room = this.client.getHabbo().getHabboInfo().getCurrentRoom();

            HabboItem item = room.getHabboItem(itemId);

            if (item == null || !(item instanceof InteractionRandomState))
                return;

            InteractionRandomState randomStateItem = (InteractionRandomState)item;
            randomStateItem.onRandomStateClick(this.client, room);
        } catch (Exception e) {
            log.error("Caught exception", e);
        }
    }
}

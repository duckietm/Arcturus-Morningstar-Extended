package com.eu.habbo.messages.incoming.rooms.items.jukebox;

import com.eu.habbo.habbohotel.items.interactions.InteractionMusicDisc;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.incoming.MessageHandler;

public class JukeBoxAddSoundTrackEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        if (!this.client.getHabbo().getHabboInfo().getCurrentRoom().hasRights(this.client.getHabbo())) return;

        int itemId = this.packet.readInt();
        int slotId = this.packet.readInt();

        Habbo habbo = this.client.getHabbo();

        if (habbo != null) {
            HabboItem item = habbo.getInventory().getItemsComponent().getHabboItem(itemId);

            if (item instanceof InteractionMusicDisc && item.getRoomId() == 0) {
                this.client.getHabbo().getHabboInfo().getCurrentRoom().getTraxManager().addSong((InteractionMusicDisc) item, habbo);
            }
        }
    }
}
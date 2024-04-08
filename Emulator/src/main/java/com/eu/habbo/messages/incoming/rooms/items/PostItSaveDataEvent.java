package com.eu.habbo.messages.incoming.rooms.items;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.PostItColor;
import com.eu.habbo.habbohotel.items.interactions.InteractionPostIt;
import com.eu.habbo.habbohotel.modtool.ScripterManager;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.incoming.MessageHandler;

import java.util.Arrays;
import java.util.List;

public class PostItSaveDataEvent extends MessageHandler {
    private static List<String> COLORS = Arrays.asList("9CCEFF", "FF9CFF", "9CFF9C", "FFFF33");

    @Override
    public void handle() throws Exception {
        int itemId = this.packet.readInt();
        String color = this.packet.readString();
        String text = this.packet.readString().replace(((char) 9) + "", "");

        if (text.length() > Emulator.getConfig().getInt("postit.charlimit")) {
            ScripterManager.scripterDetected(this.client, Emulator.getTexts().getValue("scripter.warning.sticky.size").replace("%username%", this.client.getHabbo().getHabboInfo().getUsername()).replace("%amount%", text.length() + "").replace("%limit%", Emulator.getConfig().getInt("postit.charlimit") + ""));
            return;
        }

        if (!COLORS.contains(color)) {
            return;
        }

        Room room = this.client.getHabbo().getHabboInfo().getCurrentRoom();

        if (room == null)
            return;

        HabboItem item = room.getHabboItem(itemId);

        if (!(item instanceof InteractionPostIt))
            return;

        if (!color.equalsIgnoreCase(PostItColor.YELLOW.hexColor) && !room.hasRights(this.client.getHabbo())) {
            if (!text.startsWith(item.getExtradata().replace(item.getExtradata().split(" ")[0], ""))) {
                return;
            }
        } else {
            if (!room.hasRights(this.client.getHabbo()))
                return;
        }

        if (color.isEmpty())
            color = PostItColor.YELLOW.hexColor;

        item.setUserId(room.getOwnerId());
        item.setExtradata(color + " " + text);
        item.needsUpdate(true);
        room.updateItem(item);
        Emulator.getThreading().run(item);
    }
}

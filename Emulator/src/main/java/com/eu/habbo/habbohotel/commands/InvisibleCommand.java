package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomLayout;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserRemoveComposer;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserStatusComposer;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUsersComposer;
import com.eu.habbo.threading.runnables.RoomUnitTeleport;

public class InvisibleCommand extends Command {
    public InvisibleCommand() {
        super("cmd_invisible", Emulator.getTexts().getValue("commands.keys.cmd_invisible").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        HabboInfo habboInfo = gameClient.getHabbo().getHabboInfo();
        RoomUnit roomUnit = gameClient.getHabbo().getRoomUnit();

        habboInfo.toggleInvisibility();

        if (!habboInfo.isInvisibleInRooms() && !roomUnit.isInRoom()) {
            RoomLayout roomLayout = roomUnit.getRoom().getLayout();

            new RoomUnitTeleport(roomUnit, roomUnit.getRoom(), roomLayout.getDoorTile().x, roomLayout.getDoorTile().y, roomLayout.getDoorTile().z, 0).run();

            roomUnit.setInRoom(true);

            roomUnit.getRoom().sendComposer(new RoomUsersComposer(gameClient.getHabbo()).compose());
            roomUnit.getRoom().sendComposer(new RoomUserStatusComposer(roomUnit).compose());

            WiredHandler.handle(WiredTriggerType.ENTER_ROOM, roomUnit, roomUnit.getRoom(), null);
            roomUnit.getRoom().habboEntered(gameClient.getHabbo());

            gameClient.getHabbo().whisper("Je bent weer zichtbaar voor anderen.");

            return true;
        }

        gameClient.getHabbo().whisper("Je bent nu onzichtbaar. Typ opnieuw :invisible om weer zichtbaar te zijn in kamers.");
        gameClient.getHabbo().getHabboInfo().getCurrentRoom().sendComposer(new RoomUserRemoveComposer(roomUnit).compose());

        return true;
    }
}

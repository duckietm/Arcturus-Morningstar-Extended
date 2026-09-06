package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.rooms.RoomUserRotation;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserStatusComposer;

public class LayCommand extends Command {
    public LayCommand() {
        super(null, Emulator.getTexts().getValue("commands.keys.cmd_lay").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        // ASTRO_LAY_COMMAND_FIX_V1
        if (gameClient == null || gameClient.getHabbo() == null) {
            return false;
        }

        var habbo = gameClient.getHabbo();
        var unit = habbo.getRoomUnit();
        var room = habbo.getHabboInfo().getCurrentRoom();

        if (unit == null || room == null || !unit.canForcePosture()) {
            return true;
        }

        unit.cmdSit = false;
        unit.cmdStand = false;
        unit.cmdLay = true;

        int rotation = unit.getBodyRotation().getValue();
        unit.setBodyRotation(RoomUserRotation.values()[rotation - rotation % 2]);
        unit.removeStatus(RoomUnitStatus.SIT);
        unit.setStatus(RoomUnitStatus.LAY, "0.5");

        room.sendComposer(new RoomUserStatusComposer(unit).compose());
        return true;
    }
}
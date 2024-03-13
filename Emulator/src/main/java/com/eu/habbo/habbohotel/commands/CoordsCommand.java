package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;

public class CoordsCommand extends Command {

    public CoordsCommand() {
        super("cmd_coords", Emulator.getTexts().getValue("commands.keys.cmd_coords").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (gameClient.getHabbo().getRoomUnit() == null || gameClient.getHabbo().getHabboInfo().getCurrentRoom() == null)
            return false;

        if (params.length == 1) {
            gameClient.getHabbo().alert(Emulator.getTexts().getValue("commands.generic.cmd_coords.title") + "\r\n" +
                    "x: " + gameClient.getHabbo().getRoomUnit().getX() + "\r" +
                    "y: " + gameClient.getHabbo().getRoomUnit().getY() + "\r" +
                    "z: " + (gameClient.getHabbo().getRoomUnit().hasStatus(RoomUnitStatus.SIT) ? gameClient.getHabbo().getRoomUnit().getStatus(RoomUnitStatus.SIT) : gameClient.getHabbo().getRoomUnit().getZ()) + "\r" +
                    Emulator.getTexts().getValue("generic.rotation.head") + ": " + gameClient.getHabbo().getRoomUnit().getHeadRotation() + "-" + gameClient.getHabbo().getRoomUnit().getHeadRotation().getValue() + "\r" +
                    Emulator.getTexts().getValue("generic.rotation.body") + ": " + gameClient.getHabbo().getRoomUnit().getBodyRotation() + "-" + gameClient.getHabbo().getRoomUnit().getBodyRotation().getValue() + "\r" +
                    Emulator.getTexts().getValue("generic.sitting") + ": " + (gameClient.getHabbo().getRoomUnit().hasStatus(RoomUnitStatus.SIT) ? Emulator.getTexts().getValue("generic.yes") : Emulator.getTexts().getValue("generic.no")) + "\r" +
                    "Tile State: " + gameClient.getHabbo().getHabboInfo().getCurrentRoom().getLayout().getTile(gameClient.getHabbo().getRoomUnit().getX(), gameClient.getHabbo().getRoomUnit().getY()).state.name() + "\r" +
                    "Tile Walkable: " + gameClient.getHabbo().getHabboInfo().getCurrentRoom().getLayout().getTile(gameClient.getHabbo().getRoomUnit().getX(), gameClient.getHabbo().getRoomUnit().getY()).isWalkable() + "\r" +
                    "Tile relative height: " + gameClient.getHabbo().getHabboInfo().getCurrentRoom().getLayout().getTile(gameClient.getHabbo().getRoomUnit().getX(), gameClient.getHabbo().getRoomUnit().getY()).relativeHeight() + "\r" +
                    "Tile stack height: " + gameClient.getHabbo().getHabboInfo().getCurrentRoom().getLayout().getTile(gameClient.getHabbo().getRoomUnit().getX(), gameClient.getHabbo().getRoomUnit().getY()).getStackHeight());

        } else {
            RoomTile tile = gameClient.getHabbo().getHabboInfo().getCurrentRoom().getLayout().getTile(Short.valueOf(params[1]), Short.valueOf(params[2]));

            if (tile != null) {
                gameClient.getHabbo().alert(Emulator.getTexts().getValue("commands.generic.cmd_coords.title") + "\r\n" +
                        "x: " + tile.x + "\r" +
                        "y: " + tile.y + "\r" +
                        "z: " + tile.z + "\r" +
                        "Tile State: " + tile.state.name() + "\r" +
                        "Tile Relative Height: " + tile.relativeHeight() + "\r" +
                        "Tile Stack Height: " + tile.getStackHeight() + "\r" +
                        "Tile Walkable: " + (tile.isWalkable() ? "Yes" : "No") + "\r");
            } else {
                gameClient.getHabbo().whisper(Emulator.getTexts().getValue("generic.tile.not.exists"));
            }
        }
        return true;
    }
}

package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.HotelHomeRoomService;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.rooms.RoomManager;
import java.sql.SQLException;

/** Sets the current (or specified) room as the home room for the whole hotel. */
public final class SetHomeCommand extends Command {
    public SetHomeCommand() {
        super("cmd_set_home", new String[] {"sethome", "set_home", "impostacasa"});
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        RoomManager roomManager = Emulator.getGameEnvironment().getRoomManager();
        Room room = gameClient.getHabbo().getHabboInfo().getCurrentRoom();
        boolean loadedForCommand = false;

        if (params.length >= 2) {
            int roomId;
            try {
                roomId = Integer.parseInt(params[1]);
            } catch (NumberFormatException exception) {
                gameClient.getHabbo().whisper(
                        "Uso: :sethome oppure :sethome <roomId>", RoomChatMessageBubbles.ALERT);
                return true;
            }

            room = roomManager.getRoom(roomId);
            if (room == null) {
                room = roomManager.loadRoom(roomId, false);
                loadedForCommand = room != null;
            }
        }

        if (room == null) {
            gameClient.getHabbo().whisper(
                    "Entra in una stanza oppure usa :sethome <roomId>.", RoomChatMessageBubbles.ALERT);
            return true;
        }

        try {
            int updatedUsers = HotelHomeRoomService.setForEveryone(room.getId());
            gameClient.getHabbo().alert("<b>Home room aggiornata!</b>\r"
                    + room.getName() + " (#" + room.getId() + ")\r"
                    + "Applicata a " + updatedUsers + " account esistenti, agli utenti online e ai nuovi account.");
        } catch (SQLException exception) {
            gameClient.getHabbo().whisper(
                    "Impossibile aggiornare la home room.", RoomChatMessageBubbles.ALERT);
        } finally {
            if (loadedForCommand) {
                roomManager.unloadRoom(room);
            }
        }

        return true;
    }
}

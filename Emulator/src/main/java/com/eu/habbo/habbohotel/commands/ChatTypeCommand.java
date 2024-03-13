package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.messages.outgoing.users.MeMenuSettingsComposer;

public class ChatTypeCommand extends Command {
    public ChatTypeCommand() {
        super("cmd_chatcolor", Emulator.getTexts().getValue("commands.keys.cmd_chatcolor").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {

        if (params.length >= 2) {
            int chatColor;
            try {
                chatColor = Integer.valueOf(params[1]);
            } catch (Exception e) {
                gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_chatcolor.numbers"), RoomChatMessageBubbles.ALERT);
                return true;
            }

            if (RoomChatMessageBubbles.values().length < chatColor) {
                chatColor = 0;
            }

            if (chatColor < 0) {
                gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_chatcolor.numbers"), RoomChatMessageBubbles.ALERT);
                return true;
            }

            if (!gameClient.getHabbo().hasPermission(Permission.ACC_ANYCHATCOLOR)) {
                for (String s : Emulator.getConfig().getValue("commands.cmd_chatcolor.banned_numbers").split(";")) {
                    if (Integer.valueOf(s) == chatColor) {
                        gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_chatcolor.banned"), RoomChatMessageBubbles.ALERT);
                        return true;
                    }
                }
            }

            gameClient.getHabbo().getHabboStats().chatColor = RoomChatMessageBubbles.getBubble(chatColor);
            gameClient.sendResponse(new MeMenuSettingsComposer(gameClient.getHabbo()));
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.succes.cmd_chatcolor.set").replace("%chat%", RoomChatMessageBubbles.values()[chatColor].name().replace("_", " ").toLowerCase()), RoomChatMessageBubbles.ALERT);
            return true;
        } else {
            gameClient.getHabbo().getHabboStats().chatColor = RoomChatMessageBubbles.NORMAL;
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.succes.cmd_chatcolor.reset"), RoomChatMessageBubbles.ALERT);
            return true;
        }
    }
}

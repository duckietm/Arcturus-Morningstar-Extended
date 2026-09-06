package com.eu.habbo.messages.rcon;

import com.eu.habbo.Emulator;
import com.google.gson.Gson;

/** CUSTOM RCON key "updatechatbubbles": reload the chat_bubbles table (availability, ranks, dynamic ids). */
public class UpdateChatBubbles extends RCONMessage<UpdateChatBubbles.JSONUpdateChatBubbles> {
    public UpdateChatBubbles() {
        super(JSONUpdateChatBubbles.class);
    }

    @Override
    public void handle(Gson gson, JSONUpdateChatBubbles json) {
        Emulator.getGameEnvironment().getRoomChatBubbleManager().reload();
    }

    static class JSONUpdateChatBubbles {
    }
}

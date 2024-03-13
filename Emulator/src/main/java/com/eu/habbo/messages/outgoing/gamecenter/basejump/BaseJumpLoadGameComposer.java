package com.eu.habbo.messages.outgoing.gamecenter.basejump;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class BaseJumpLoadGameComposer extends MessageComposer {
    public static String FASTFOOD_KEY = "";

    private final GameClient client;
    private final int game;

    public BaseJumpLoadGameComposer(GameClient client, int game) {
        this.client = client;
        this.game = game;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.BaseJumpLoadGameComposer);

        if (this.game == 3) {
            this.response.appendInt(3);
            this.response.appendString("basejump");
            this.response.appendString(Emulator.getConfig().getValue("basejump.url"));
            this.response.appendString("best");
            this.response.appendString("showAll");
            this.response.appendInt(60);
            this.response.appendInt(10);
            this.response.appendInt(0);
            this.response.appendInt(6);
            this.response.appendString("assetUrl");
            this.response.appendString(Emulator.getConfig().getValue("basejump.assets.url"));
            this.response.appendString("habboHost");
            this.response.appendString(Emulator.getConfig().getValue("hotel.url"));
            this.response.appendString("accessToken");
            this.response.appendString(Emulator.getConfig().getValue("username") + "\t" + Emulator.version + "\t" + this.client.getHabbo().getHabboInfo().getId() + "\t" + this.client.getHabbo().getHabboInfo().getUsername() + "\t" + this.client.getHabbo().getHabboInfo().getLook() + "\t" + this.client.getHabbo().getHabboInfo().getCredits() + "\t" + FASTFOOD_KEY);
            this.response.appendString("gameServerHost");
            this.response.appendString("google.com");
            this.response.appendString("gameServerPort");
            this.response.appendString("3002");
            this.response.appendString("socketPolicyPort");
            this.response.appendString("3002");


        }
        return this.response;
    }
}
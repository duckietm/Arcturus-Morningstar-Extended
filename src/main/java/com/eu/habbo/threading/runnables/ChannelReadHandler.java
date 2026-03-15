package com.eu.habbo.threading.runnables;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.networking.gameserver.GameServerAttributes;
import io.netty.channel.ChannelHandlerContext;

public class ChannelReadHandler implements Runnable {

    private final ChannelHandlerContext ctx;
    private final ClientMessage message;

    public ChannelReadHandler(ChannelHandlerContext ctx, ClientMessage message) {
        this.ctx = ctx;
        this.message = message;
    }

    public void run() {
        try {
            GameClient client = this.ctx.channel().attr(GameServerAttributes.CLIENT).get();

            if (client != null) {
                Emulator.getGameServer().getPacketManager().handlePacket(client, message);
            }
        } finally {
            this.message.release();
        }
    }
}

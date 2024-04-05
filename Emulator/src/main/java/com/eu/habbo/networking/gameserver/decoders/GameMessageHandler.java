package com.eu.habbo.networking.gameserver.decoders;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.PacketManager;
import com.eu.habbo.threading.runnables.ChannelReadHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.TooLongFrameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@ChannelHandler.Sharable
public class GameMessageHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameMessageHandler.class);


    @Override
    public void channelRegistered(ChannelHandlerContext ctx) {
        if (!Emulator.getGameServer().getGameClientManager().addClient(ctx)) {
            ctx.channel().close();
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) {
        ctx.channel().close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ClientMessage message = (ClientMessage) msg;

        try {
            ChannelReadHandler handler = new ChannelReadHandler(ctx, message);

            if (PacketManager.MULTI_THREADED_PACKET_HANDLING) {
                Emulator.getThreading().run(handler);
                return;
            }

            handler.run();
        } catch (Exception e) {
            LOGGER.error("Caught exception", e);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof IOException) {
            ctx.channel().close();
            return;
        }
        if (Emulator.getConfig().getBoolean("debug.mode")) {
            if (cause instanceof TooLongFrameException) {
                LOGGER.error("Disconnecting client, reason: \"" + cause.getMessage() + "\".");
            } else {
                LOGGER.error("Disconnecting client, exception in GameMessageHander.", cause);
            }
        }
        ctx.channel().close();
    }

}
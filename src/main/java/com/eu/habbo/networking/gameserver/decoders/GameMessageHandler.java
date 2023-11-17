package com.eu.habbo.networking.gameserver.decoders;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.PacketManager;
import com.eu.habbo.threading.runnables.ChannelReadHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.UnsupportedMessageTypeException;
import io.netty.handler.ssl.NotSslRecordException;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

@ChannelHandler.Sharable
@Slf4j
public class GameMessageHandler extends ChannelInboundHandlerAdapter {
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
            log.error("Caught exception", e);
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
            String clientIpAddress = ctx.channel().remoteAddress().toString();
            String xForwardedFor = (String) ctx.channel().attr(AttributeKey.valueOf("X-Forwarded-For")).get();
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                clientIpAddress = xForwardedFor;
            }
            clientIpAddress = clientIpAddress.substring(1, clientIpAddress.lastIndexOf(':'));

            if (cause instanceof NotSslRecordException) {
                log.error("Someone speaks transport plaintext instead of SSL, IP: {}", clientIpAddress);
            }
            else if (cause instanceof DecoderException) {
                log.error("Someone speaks transport plaintext instead of SSL, IP: {}", clientIpAddress);
            }
            else if (cause instanceof TooLongFrameException) {
                log.error("Disconnecting client, reason: {}", cause.getMessage());
            }
            else if (cause instanceof SSLHandshakeException) {
                log.error("URL Request error from source: {}",  clientIpAddress);
            }
            else if (cause instanceof NoSuchAlgorithmException) {
                log.error("Invalid SSL algorithm, only TLSv1.2 supported in the request, IP: {}", clientIpAddress);
            }
            else if (cause instanceof KeyManagementException) {
                log.error("Invalid SSL algorithm, only TLSv1.2 supported in the request, IP: {}", clientIpAddress);
            }
            else if (cause instanceof UnsupportedMessageTypeException) {
                log.error("There was an illegal SSL request from (X-forwarded-for/CF-Connecting-IP has not being injected yet!) {}",  clientIpAddress);
            }
            else if (cause instanceof SSLException) {
                log.error("SSL Problem: {}", cause.getMessage() + cause);
            }
            else {
                log.error("Disconnecting client, exception in GameMessageHandler.", cause);
            }
        }
        ctx.channel().close();
    }
}
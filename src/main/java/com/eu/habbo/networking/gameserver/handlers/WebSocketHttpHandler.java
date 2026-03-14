package com.eu.habbo.networking.gameserver.handlers;

import com.eu.habbo.Emulator;
import com.eu.habbo.networking.gameserver.GameServerAttributes;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;

import java.net.URI;

public class WebSocketHttpHandler extends ChannelInboundHandlerAdapter {
    private static final String ORIGIN_HEADER = "Origin";

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpMessage) {
            if (!handleHttpRequest(ctx, (HttpMessage) msg)) {
                ReferenceCountUtil.release(msg);
                return;
            }
        }
        super.channelRead(ctx, msg);
        ctx.pipeline().remove(this);
    }

    private boolean handleHttpRequest(ChannelHandlerContext ctx, HttpMessage req) {
        String origin = "error";

        try {
            if (req.headers().contains(ORIGIN_HEADER)) {
                origin = getDomainNameFromUrl(req.headers().get(ORIGIN_HEADER));
            }
        } catch (Exception ignored) {
        }

        String whitelist = Emulator.getConfig().getValue("ws.whitelist", "localhost");
        if (!isWhitelisted(origin, whitelist.split(","))) {
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.FORBIDDEN,
                    Unpooled.wrappedBuffer("Origin forbidden".getBytes())
            );
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            return false;
        }

        String ipHeader = Emulator.getConfig().getValue("ws.ip.header", "");
        if (!ipHeader.isEmpty() && req.headers().contains(ipHeader)) {
            String ip = req.headers().get(ipHeader);
            ctx.channel().attr(GameServerAttributes.WS_IP).set(ip);
        }

        return true;
    }

    private static String getDomainNameFromUrl(String url) throws Exception {
        URI uri = new URI(url);
        String domain = uri.getHost();
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

    private static boolean isWhitelisted(String toCheck, String[] whitelist) {
        for (String entry : whitelist) {
            String trimmed = entry.trim();
            if (trimmed.equals("*")) {
                return true;
            }
            if (trimmed.startsWith("*")) {
                String suffix = trimmed.substring(1);
                if (toCheck.endsWith(suffix) || ("." + toCheck).equals(suffix)) {
                    return true;
                }
            } else {
                if (toCheck.equals(trimmed)) {
                    return true;
                }
            }
        }
        return false;
    }
}

package org.krews.plugin.nitro.websockets.handlers;

import com.eu.habbo.Emulator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import org.krews.plugin.nitro.Utils;
import org.krews.plugin.nitro.main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomHTTPHandler extends ChannelInboundHandlerAdapter {
    private static final String ORIGIN_HEADER = "Origin";
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomHTTPHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof HttpMessage) {
            if(!handleHttpRequest(ctx, (HttpMessage) msg))
            {
                ReferenceCountUtil.release(msg);//discard message
                return;
            }
        }
        super.channelRead(ctx, msg);
        ctx.pipeline().remove(this);
    }

    public boolean handleHttpRequest(ChannelHandlerContext ctx, HttpMessage req) {
        String origin = "error";
        String clientIP;
        String header = Emulator.getConfig().getValue("ws.nitro.ip.header", "");

        // Check if X-Forwarded-For header is present and use the first IP as the client IP if available
        if (req.headers().contains(header)) {
            String xForwardedFor = req.headers().get(header);
            clientIP = xForwardedFor.split(",")[0].trim(); // Get the first IP in the list
        } else {
            clientIP = ctx.channel().remoteAddress().toString().substring(1).split(":")[0]; // Get the client's IP address
        }

        try {
            if(req.headers().contains(ORIGIN_HEADER)) {
                origin = Utils.getDomainNameFromUrl(req.headers().get(ORIGIN_HEADER));
            }
        } catch (Exception ignored) { }

        if(!Utils.isWhitelisted(origin, Emulator.getConfig().getValue("websockets.whitelist", "localhost").split(","))) {
            LOGGER.warn("Access denied for IP: {}", clientIP);

            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN, Unpooled.wrappedBuffer("W.T.F are you doing?".getBytes()));
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            return false;
        }

        if(!header.isEmpty() && req.headers().contains(header)) {
            String ip = req.headers().get(header);
            ctx.channel().attr(main.WS_IP).set(ip);
        }
        return true;
    }
}
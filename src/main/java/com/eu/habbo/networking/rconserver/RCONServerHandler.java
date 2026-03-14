package com.eu.habbo.networking.rconserver;


import com.eu.habbo.Emulator;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RCONServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RCONServerHandler.class);

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        String adress = ctx.channel().remoteAddress().toString().split(":")[0].replace("/", "");

        for (String s : Emulator.getRconServer().allowedAdresses) {
            if (s.equalsIgnoreCase(adress)) {
                return;
            }
        }

        ctx.channel().close();

        LOGGER.warn("RCON Remote connection closed: {}. IP not allowed!", adress);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf data = (ByteBuf) msg;

        byte[] d = new byte[data.readableBytes()];
        data.getBytes(0, d);
        String message = new String(d);
        Gson gson = new Gson();
        String response = "ERROR";
        String key = "";
        try {
            JsonObject object = gson.fromJson(message, JsonObject.class);
            key = object.get("key").getAsString();
            response = Emulator.getRconServer().handle(ctx, key, object.get("data").toString());
        } catch (ArrayIndexOutOfBoundsException e) {
            LOGGER.error("Unknown RCON Message: {}", key);
        } catch (Exception e) {
            LOGGER.error("Invalid RCON Message: {}", message);
            e.printStackTrace();
        }

        ChannelFuture f = ctx.channel().write(Unpooled.copiedBuffer(response.getBytes()), ctx.channel().voidPromise());
        ctx.channel().flush();
        ctx.flush();
        f.channel().close();
        data.release();
    }
}

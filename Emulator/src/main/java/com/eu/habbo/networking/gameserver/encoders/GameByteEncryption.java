package com.eu.habbo.networking.gameserver.encoders;

import com.eu.habbo.networking.gameserver.GameServerAttributes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;

public class GameByteEncryption extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        // convert to Bytebuf
        ByteBuf in = (ByteBuf) msg;

        // read available bytes
        ByteBuf data = (in).readBytes(in.readableBytes());

        //release old object
        ReferenceCountUtil.release(in);

        // Encrypt.
        ctx.channel().attr(GameServerAttributes.CRYPTO_SERVER).get().parse(data.array());

        // Continue in the pipeline.
        ctx.write(data, promise);
    }
}

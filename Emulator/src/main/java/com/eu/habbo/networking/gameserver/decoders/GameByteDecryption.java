package com.eu.habbo.networking.gameserver.decoders;

import com.eu.habbo.networking.gameserver.GameServerAttributes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class GameByteDecryption extends ByteToMessageDecoder {

    public GameByteDecryption() {
        setSingleDecode(true);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // Read all available bytes.
        ByteBuf data = in.readBytes(in.readableBytes());

        // Decrypt.
        ctx.channel().attr(GameServerAttributes.CRYPTO_CLIENT).get().parse(data.array());

        // Continue in the pipeline.
        out.add(data);
    }

}

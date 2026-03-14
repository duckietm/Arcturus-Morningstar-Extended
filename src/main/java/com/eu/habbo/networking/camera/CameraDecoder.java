package com.eu.habbo.networking.camera;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

class CameraDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> objects) {
        int readerIndex = byteBuf.readerIndex();
        if (byteBuf.readableBytes() < 6) {
            byteBuf.readerIndex(readerIndex);
            return;
        }

        int length = byteBuf.readInt();
        byteBuf.readerIndex(readerIndex);

        if (byteBuf.readableBytes() < (length)) {
            byteBuf.readerIndex(readerIndex);
            return;
        }

        byteBuf.readerIndex(readerIndex);
        objects.add(byteBuf.readBytes(length + 4));
    }
}
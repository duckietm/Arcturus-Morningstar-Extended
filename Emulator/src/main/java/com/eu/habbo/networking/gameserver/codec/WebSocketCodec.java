package com.eu.habbo.networking.gameserver.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import java.util.List;

/**
 * Converts the raw Habbo/EvaWire byte stream to WebSocket binary messages.
 *
 * Large server packets are deliberately split into multiple independent
 * WebSocket messages. The Nitro client already buffers incoming ArrayBuffers
 * until the complete EvaWire packet length is available, so splitting here
 * preserves the game protocol while avoiding proxy/tunnel per-message limits.
 */
public class WebSocketCodec extends MessageToMessageCodec<WebSocketFrame, ByteBuf> {
    /**
     * Keep each individual WebSocket message comfortably below 64 KiB.
     */
    private static final int MAX_OUTBOUND_WEBSOCKET_MESSAGE_BYTES = 48 * 1024;

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (!in.isReadable()) {
            return;
        }

        int offset = in.readerIndex();
        int remaining = in.readableBytes();

        while (remaining > 0) {
            int chunkLength = Math.min(
                    remaining,
                    MAX_OUTBOUND_WEBSOCKET_MESSAGE_BYTES);

            ByteBuf chunk = in.retainedSlice(offset, chunkLength);

            out.add(new BinaryWebSocketFrame(chunk));

            offset += chunkLength;
            remaining -= chunkLength;
        }
    }

    @Override
    protected void decode(
            ChannelHandlerContext ctx,
            WebSocketFrame in,
            List<Object> out) {

        if (!(in instanceof BinaryWebSocketFrame) || !in.isFinalFragment()) {
            ctx.close();
            return;
        }

        out.add(in.content().retain());
    }
}
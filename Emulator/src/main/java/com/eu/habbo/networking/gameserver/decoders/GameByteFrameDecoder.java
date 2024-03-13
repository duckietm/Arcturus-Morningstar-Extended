package com.eu.habbo.networking.gameserver.decoders;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class GameByteFrameDecoder extends LengthFieldBasedFrameDecoder {

    private static final int MAX_PACKET_LENGTH = 2097152;  // 2 MB
    private static final int LENGTH_FIELD_OFFSET = 0;
    private static final int LENGTH_FIELD_LENGTH = 4;
    private static final int LENGTH_FIELD_ADJUSTMENT = 0;
    private static final int INITIAL_BYTES_TO_STRIP = 4;

    public GameByteFrameDecoder() {
        super(MAX_PACKET_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH, LENGTH_FIELD_ADJUSTMENT, INITIAL_BYTES_TO_STRIP);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        if (in.readableBytes() < LENGTH_FIELD_LENGTH) {
            // Wait until we have at least LENGTH_FIELD_LENGTH bytes available
            return null;
        }

        int packetLength = in.getInt(in.readerIndex());
        if (packetLength > MAX_PACKET_LENGTH) {
            // Packet exceeds limit, could be an indication of problem or attack
            ctx.close();  // Close  channel
            return null;
        }

        if (in.readableBytes() < LENGTH_FIELD_LENGTH + packetLength) {
            // Wait until we have all packet bytes available
            return null;
        }

        // Everything seems to be in order, continue with normal decoding
        return super.decode(ctx, in);
    }
}

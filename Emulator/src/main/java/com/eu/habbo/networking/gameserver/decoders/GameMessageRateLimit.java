package com.eu.habbo.networking.gameserver.decoders;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.networking.gameserver.GameServerAttributes;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;
import java.util.logging.Logger;

public class GameMessageRateLimit extends MessageToMessageDecoder<ClientMessage> {
    private static final int RESET_TIME = 1;
    private static final int MAX_COUNTER = 10;
    private static final Logger logger = Logger.getLogger(GameMessageRateLimit.class.getName());

    @Override
    protected void decode(ChannelHandlerContext ctx, ClientMessage message, List<Object> out) throws Exception {
        GameClient client = ctx.channel().attr(GameServerAttributes.CLIENT).get();

        if (client == null) {
            return;
        }

        int timestamp = Emulator.getIntUnixTimestamp();
        if (timestamp - client.lastPacketCounterCleared > RESET_TIME) {
            client.incomingPacketCounter.clear();
            client.lastPacketCounterCleared = timestamp;
        }

        int count = client.incomingPacketCounter.getOrDefault(message.getMessageId(), 0);

        if (count > MAX_COUNTER) {
            String userIP = ctx.channel().remoteAddress().toString();
            logger.warning(String.format("User with IP %s exceeded the message rate limit for message ID %s", userIP, message.getMessageId()));
            return;
        }

        client.incomingPacketCounter.put(message.getMessageId(), count + 1);

        out.add(message);
    }
}

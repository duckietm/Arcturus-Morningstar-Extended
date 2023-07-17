package com.eu.habbo.networking.gameserver.decoders;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.PacketNames;
import com.eu.habbo.util.ANSI;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
public class GameClientMessageLogger extends MessageToMessageDecoder<ClientMessage> {
    private final PacketNames names;

    public GameClientMessageLogger()  {
        this.names = Emulator.getGameServer().getPacketManager().getNames();
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ClientMessage message, List<Object> out) {
        log.debug(String.format("[" + ANSI.GREEN + "CLIENT" + ANSI.DEFAULT + "][%-4d][%-41s] => %s",
                message.getMessageId(),
                this.names.getIncomingName(message.getMessageId()),
                message.getMessageBody()));

        out.add(message);
    }

}

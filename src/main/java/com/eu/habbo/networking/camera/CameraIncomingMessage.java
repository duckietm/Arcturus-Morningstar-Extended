package com.eu.habbo.networking.camera;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

import java.nio.charset.Charset;

public abstract class CameraIncomingMessage extends CameraMessage {
    public CameraIncomingMessage(Short header, ByteBuf body) {
        super(header);
        this.buffer.writerIndex(0).writeBytes(body);
    }

    public int readShort() {
        return this.buffer.readShort();
    }

    public Integer readInt() {
        try {
            return this.buffer.readInt();
        } catch (Exception e) {
        }

        return 0;
    }

    public boolean readBoolean() {
        try {
            return this.buffer.readByte() == 1;
        } catch (Exception e) {
        }

        return false;
    }


    public String readString() {
        try {
            int length = this.readInt();
            byte[] data = new byte[length];
            this.buffer.readBytes(data);
            return new String(data);
        } catch (Exception e) {
            return "";
        }
    }

    public String getMessageBody() {
        String consoleText = this.buffer.toString(Charset.defaultCharset());

        for (int i = -1; i < 31; i++) {
            consoleText = consoleText.replace(Character.toString((char) i), "[" + i + "]");
        }

        return consoleText;
    }

    public int bytesAvailable() {
        return this.buffer.readableBytes();
    }

    public abstract void handle(Channel client) throws Exception;
}

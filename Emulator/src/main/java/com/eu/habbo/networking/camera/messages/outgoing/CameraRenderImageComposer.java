package com.eu.habbo.networking.camera.messages.outgoing;

import com.eu.habbo.Emulator;
import com.eu.habbo.networking.camera.CameraOutgoingMessage;
import com.eu.habbo.networking.camera.messages.CameraOutgoingHeaders;
import io.netty.channel.Channel;

public class CameraRenderImageComposer extends CameraOutgoingMessage {
    public final int timestamp;
    final int userId;
    final int backgroundColor;
    final int width;
    final int height;
    final String JSON;

    public CameraRenderImageComposer(int userId, int backgroundColor, int width, int height, String json) {
        super(CameraOutgoingHeaders.RenderImageComposer);

        this.userId = userId;
        this.timestamp = Emulator.getIntUnixTimestamp();
        this.backgroundColor = backgroundColor;
        this.width = width;
        this.height = height;
        this.JSON = json;
    }

    @Override
    public void compose(Channel channel) {
        this.appendInt32(this.userId);
        this.appendInt32(this.timestamp);
        this.appendInt32(this.backgroundColor);
        this.appendInt32(this.width);
        this.appendInt32(this.height);
        this.appendString(this.JSON);
    }
}
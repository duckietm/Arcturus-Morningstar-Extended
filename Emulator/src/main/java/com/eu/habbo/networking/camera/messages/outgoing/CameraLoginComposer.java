package com.eu.habbo.networking.camera.messages.outgoing;

import com.eu.habbo.Emulator;
import com.eu.habbo.networking.camera.CameraOutgoingMessage;
import com.eu.habbo.networking.camera.messages.CameraOutgoingHeaders;
import io.netty.channel.Channel;

public class CameraLoginComposer extends CameraOutgoingMessage {
    public CameraLoginComposer() {
        super(CameraOutgoingHeaders.LoginComposer);
    }

    @Override
    public void compose(Channel channel) {
        this.appendString(Emulator.getConfig().getValue("username").trim());
        this.appendString(Emulator.getConfig().getValue("password").trim());
        this.appendString(Emulator.version);
    }
}
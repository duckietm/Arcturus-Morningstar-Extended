package com.eu.habbo.messages.incoming.camera;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.camera.CameraPriceComposer;

public class RequestCameraConfigurationEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        this.client.sendResponse(new CameraPriceComposer(Emulator.getConfig().getInt("camera.price.credits"), Emulator.getConfig().getInt("camera.price.points"), Emulator.getConfig().getInt("camera.price.points.publish")));
    }
}
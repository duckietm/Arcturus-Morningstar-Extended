package com.eu.habbo.messages.incoming.camera;

import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.camera.CameraPriceComposer;

public class RequestCameraConfigurationEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        // Advertise exactly what CameraPurchaseEvent / CameraPublishToWebEvent will charge.
        this.client.sendResponse(new CameraPriceComposer(
                CameraPurchaseEvent.purchaseCredits(),
                CameraPurchaseEvent.purchasePoints(),
                CameraPublishToWebEvent.publishPoints(),
                CameraPurchaseEvent.purchasePointsType(),
                CameraPublishToWebEvent.publishPointsType()));
    }
}

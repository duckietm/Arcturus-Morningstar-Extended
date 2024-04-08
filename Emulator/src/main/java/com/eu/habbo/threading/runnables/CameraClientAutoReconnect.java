package com.eu.habbo.threading.runnables;

import com.eu.habbo.Emulator;
import com.eu.habbo.networking.camera.CameraClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CameraClientAutoReconnect implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CameraClientAutoReconnect.class);

    @Override
    public void run() {
        if (CameraClient.attemptReconnect && !Emulator.isShuttingDown) {
            if (!(CameraClient.channelFuture != null && CameraClient.channelFuture.channel().isRegistered())) {
                LOGGER.info("Attempting to connect to the Camera server.");
                if (Emulator.getCameraClient() != null) {
                    Emulator.getCameraClient().disconnect();
                } else {
                    Emulator.setCameraClient(new CameraClient());
                }

                try {
                    Emulator.getCameraClient().connect();
                } catch (Exception e) {
                    LOGGER.error("Failed to start the camera client.", e);
                }
            } else {
                CameraClient.attemptReconnect = false;
                LOGGER.info("Already connected to the camera. Reconnecting not needed!");
            }
        }

        Emulator.getThreading().run(this, 5000);
    }
}
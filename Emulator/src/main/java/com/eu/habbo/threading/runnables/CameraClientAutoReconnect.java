package com.eu.habbo.threading.runnables;

import com.eu.habbo.Emulator;
import com.eu.habbo.networking.camera.CameraClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CameraClientAutoReconnect implements Runnable {

    @Override
    public void run() {
        if (CameraClient.attemptReconnect && !Emulator.isShuttingDown) {
            if (!(CameraClient.channelFuture != null && CameraClient.channelFuture.channel().isRegistered())) {
                log.info("Attempting to connect to the Camera server.");
                if (Emulator.getCameraClient() != null) {
                    Emulator.getCameraClient().disconnect();
                } else {
                    Emulator.setCameraClient(new CameraClient());
                }

                try {
                    Emulator.getCameraClient().connect();
                } catch (Exception e) {
                    log.error("Failed to start the camera client.", e);
                }
            } else {
                CameraClient.attemptReconnect = false;
                log.info("Already connected to the camera. Reconnecting not needed!");
            }
        }
        Emulator.getThreading().run(this, 5000);
    }
}
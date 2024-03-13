package com.eu.habbo.core.consolecommands;

import com.eu.habbo.networking.camera.CameraClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConsoleReconnectCameraCommand extends ConsoleCommand {
    public ConsoleReconnectCameraCommand() {
        super("camera", "Attempt to reconnect to the camera server.");
    }

    @Override
    public void handle(String[] args) throws Exception {
        log.info("Connecting to the camera...");
        CameraClient.attemptReconnect = true;
    }
}
package com.eu.habbo.core.consolecommands;

import com.eu.habbo.networking.camera.CameraClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleReconnectCameraCommand extends ConsoleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleReconnectCameraCommand.class);

    public ConsoleReconnectCameraCommand() {
        super("camera", "Attempt to reconnect to the camera server.");
    }

    @Override
    public void handle(String[] args) throws Exception {
        LOGGER.info("Connecting to the camera...");
        CameraClient.attemptReconnect = true;
    }
}
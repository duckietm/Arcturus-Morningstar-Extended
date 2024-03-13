package com.eu.habbo.threading.runnables;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.ServerMessage;

public class ShutdownEmulator implements Runnable {
    public static boolean instantiated = false;
    public static int timestamp = 0;

    public ShutdownEmulator(ServerMessage message) {
        if (!instantiated) {
            instantiated = true;

            if (message != null) {
                Emulator.getGameServer().getGameClientManager().sendBroadcastResponse(message);
            }
        }
    }

    @Override
    public void run() {
        Emulator.getRuntime().exit(0);
    }
}
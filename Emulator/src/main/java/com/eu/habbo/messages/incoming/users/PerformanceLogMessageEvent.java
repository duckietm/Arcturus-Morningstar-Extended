package com.eu.habbo.messages.incoming.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PerformanceLogMessageEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        int timer = this.packet.readInt(); // kind of time since start of client
        String externalInterface = this.packet.readString(); // available ExternalInterface
        String flashVersion = this.packet.readString(); // _flashVersion
        String userOs = this.packet.readString(); // user OS
        String unknown1 = this.packet.readString(); // always an empty string, not assigned by client
        boolean debuggerState = this.packet.readBoolean(); // _SafeStr_8266 Capabilities.isDebugger, false by default
        int totalMemory = this.packet.readInt(); // System.totalMemory, Total Memory in KB
        int unknown2 = this.packet.readInt(); // by default set to -1
        int averageUpdateIntervalGarbaged = this.packet.readInt(); // averageUpdateInterval.isGarbageMonitored
        int averageUpdateInterval = this.packet.readInt(); // averageUpdateInterval
        int slowUpdateLimit = this.packet.readInt(); // exceeded slowUpdateLimit
        if (Emulator.debugging) {
            log.info("Timer: " + timer +
                    ", ExternalInterface: " + externalInterface +
                    ", FlashVersion: " + flashVersion +
                    ", UserOS: " + userOs +
                    ", Unknown1 (Empty String): '" + unknown1 + "'" +
                    ", DebuggerState: " + debuggerState +
                    ", TotalMemory (KB): " + totalMemory +
                    ", Unknown2 (Default -1): " + unknown2 +
                    ", AverageUpdateIntervalGarbaged: " + averageUpdateIntervalGarbaged +
                    ", AverageUpdateInterval: " + averageUpdateInterval +
                    ", SlowUpdateLimit: " + slowUpdateLimit);
        }
    }
}

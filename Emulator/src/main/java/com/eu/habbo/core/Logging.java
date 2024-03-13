package com.eu.habbo.core;

import com.eu.habbo.Emulator;
import lombok.extern.slf4j.Slf4j;
import java.sql.SQLException;

@Slf4j
public class Logging {
    @Deprecated
    public void logStart(Object line) {
        log.info("[LOADING] {}", line);
    }

    @Deprecated
    public void logShutdownLine(Object line) {
        log.info("[SHUTDOWN] {}", line);
    }

    @Deprecated
    public void logUserLine(Object line) {
        log.info("[USER] {}", line);
    }

    @Deprecated
    public void logDebugLine(Object line) {
        log.debug("[DEBUG] {}", line);
    }

    @Deprecated
    public void logPacketLine(Object line) {
        if (Emulator.getConfig().getBoolean("debug.show.packets")) {
            log.debug("[PACKET] {}", line);
        }
    }

    @Deprecated
    public void logUndefinedPacketLine(Object line) {
        if (Emulator.getConfig().getBoolean("debug.show.packets.undefined")) {
            log.debug("[PACKET] [UNDEFINED] {}", line);
        }
    }

    @Deprecated
    public void logErrorLine(Object line) {
        log.error("[ERROR] {}", line);
    }

    @Deprecated
    public void logSQLException(SQLException e) {
        log.error("[ERROR] SQLException", e);
    }

    @Deprecated
    public void logPacketError(Object e) {
        log.error("[ERROR] PacketError {}", e);
    }

    @Deprecated
    public void handleException(Exception e) {
        log.error("[ERROR] Exception", e);
    }

}

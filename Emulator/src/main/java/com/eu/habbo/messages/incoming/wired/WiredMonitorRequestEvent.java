package com.eu.habbo.messages.incoming.wired;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.wired.WiredMonitorDataComposer;

public class WiredMonitorRequestEvent extends MessageHandler {
    private static final int ACTION_FETCH = 0;
    private static final int ACTION_CLEAR_LOGS = 1;

    @Override
    public void handle() throws Exception {
        Room room = currentRoom();

        if (room == null) {
            return;
        }

        if (!room.canInspectWired(this.client.getHabbo())) {
            return;
        }

        int action = ACTION_FETCH;

        if (this.packet.bytesAvailable() >= 4) {
            action = this.packet.readInt();
        }

        if ((action == ACTION_CLEAR_LOGS) && room.canModifyWired(this.client.getHabbo())) {
            WiredManager.clearDiagnosticsLogs(room.getId());
        }

        this.client.sendResponse(new WiredMonitorDataComposer(WiredManager.getDiagnosticsSnapshot(room.getId())));
    }

    @Override
    public int getRatelimit() {
        return 50;
    }
}

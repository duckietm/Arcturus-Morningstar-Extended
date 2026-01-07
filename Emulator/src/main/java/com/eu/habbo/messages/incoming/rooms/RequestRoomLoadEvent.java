package com.eu.habbo.messages.incoming.rooms;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.incoming.MessageHandler;

public class RequestRoomLoadEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        int roomId = this.packet.readInt();
        String password = this.packet.readString();

        // Reset stale loadingRoom if timestamp has expired (indicates failed/stuck load)
        if (this.client.getHabbo().getHabboInfo().getLoadingRoom() != 0 
            && this.client.getHabbo().getHabboStats().roomEnterTimestamp + 5000 < System.currentTimeMillis()) {
            this.client.getHabbo().getHabboInfo().setLoadingRoom(0);
        }

        if (this.client.getHabbo().getHabboInfo().getLoadingRoom() == 0 && this.client.getHabbo().getHabboStats().roomEnterTimestamp + 1000 < System.currentTimeMillis()) {

            // Start background loading early to reduce perceived load time
            Room roomToLoad = Emulator.getGameEnvironment().getRoomManager().getRoom(roomId);
            if (roomToLoad == null) {
                roomToLoad = Emulator.getGameEnvironment().getRoomManager().loadRoom(roomId);
            }
            if (roomToLoad != null && roomToLoad.isPreLoaded() && !roomToLoad.isLoadedOrLoading()) {
                roomToLoad.startBackgroundLoad();
            }

            Room room = this.client.getHabbo().getHabboInfo().getCurrentRoom();
            if (room != null) {
                Emulator.getGameEnvironment().getRoomManager().logExit(this.client.getHabbo());

                room.removeHabbo(this.client.getHabbo(), true);

                this.client.getHabbo().getHabboInfo().setCurrentRoom(null);
            }

            if (this.client.getHabbo().getRoomUnit() != null && this.client.getHabbo().getRoomUnit().isTeleporting) {
                this.client.getHabbo().getRoomUnit().isTeleporting = false;
            }

            Emulator.getGameEnvironment().getRoomManager().enterRoom(this.client.getHabbo(), roomId, password);
        }
    }
}

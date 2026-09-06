package com.eu.habbo.messages.incoming.rooms;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.commands.AvailableCommandsComposer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestRoomLoadEvent extends MessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestRoomLoadEvent.class);

    @Override
    public void handle() throws Exception {
        int roomId = this.packet.readInt();
        String password = this.packet.readString();

        // Every entry (including a re-entry after a disconnect) starts at the door: the
        // optional client spawn coordinates are deliberately ignored.
        int spawnX = -1;
        int spawnY = -1;

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

            // Resolve spawn tile from coordinates (either from client or from saved position above)
            RoomTile spawnTile = null;

            if (spawnX >= 0 && spawnY >= 0) {
                Room targetRoom = Emulator.getGameEnvironment().getRoomManager().getRoom(roomId);
                if (targetRoom == null) {
                    targetRoom = Emulator.getGameEnvironment().getRoomManager().loadRoom(roomId);
                }
                if (targetRoom != null && targetRoom.getLayout() != null) {
                    RoomTile tile = targetRoom.getLayout().getTile((short) spawnX, (short) spawnY);
                    if (tile != null && tile.isWalkable()) {
                        spawnTile = tile;
                    }
                }
            }

            boolean isReconnect = spawnTile != null;
            LOGGER.debug("[RequestRoomLoadEvent] Entering room {} (spawnTile={}, isReconnect={})",
                    roomId,
                    spawnTile != null ? "(" + spawnTile.x + "," + spawnTile.y + ")" : "door",
                    isReconnect);
            Emulator.getGameEnvironment().getRoomManager().enterRoom(this.client.getHabbo(), roomId, password, false, spawnTile, isReconnect);

            // The initial login command packet can arrive before React mounts
            // the room chat autocomplete listener. Refresh it once the room UI
            // exists; getCommandsForRank keeps the result server-authoritative.
            var commandClient = this.client;
            Emulator.getThreading().run(() -> {
                if (commandClient.getHabbo() == null) return;

                commandClient.sendResponse(new AvailableCommandsComposer(
                        Emulator.getGameEnvironment().getCommandHandler().getCommandsForRank(
                                commandClient.getHabbo().getHabboInfo().getRank().getId())));
            }, 750);
        }
    }
}

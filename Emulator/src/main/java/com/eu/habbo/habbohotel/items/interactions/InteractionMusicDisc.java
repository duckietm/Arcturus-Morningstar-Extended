package com.eu.habbo.habbohotel.items.interactions;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.ServerMessage;
import lombok.extern.slf4j.Slf4j;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
public class InteractionMusicDisc extends HabboItem {
    private int songId;
    public InteractionMusicDisc(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);

        String[] stuff = this.getExtradata().split("\n");

        if (stuff.length >= 7 && !stuff[6].isEmpty()) {
            try {
                this.songId = Integer.valueOf(stuff[6]);
            } catch (Exception e) {
                log.error("Warning: Item " + this.getId() + " has an invalid song id set for its music disk!");
            }
        }
    }

    public InteractionMusicDisc(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);

        String[] stuff = this.getExtradata().split("\n");

        if (stuff.length >= 7 && !stuff[6].isEmpty()) {
            try {
                this.songId = Integer.valueOf(stuff[6]);
            } catch (Exception e) {
                log.error("Warning: Item " + this.getId() + " has an invalid song id set for its music disk!");
            }
        }
    }

    @Override
    public void serializeExtradata(ServerMessage serverMessage) {
        serverMessage.appendInt((this.isLimited() ? 256 : 0));
        serverMessage.appendString(this.getExtradata());

        super.serializeExtradata(serverMessage);
    }

    @Override
    public boolean canWalkOn(RoomUnit roomUnit, Room room, Object[] objects) {
        return false;
    }

    @Override
    public boolean isWalkable() {
        return false;
    }

    @Override
    public void onWalk(RoomUnit roomUnit, Room room, Object[] objects) throws Exception {

    }

    public int getSongId() {
        return this.songId;
    }

    @Override
    public void onPlace(Room room) {
        super.onPlace(room);

        room.getTraxManager().sendUpdatedSongList();
    }

    @Override
    public void onPickUp(Room room) {
        super.onPickUp(room);

        room.getTraxManager().sendUpdatedSongList();
    }
}

package com.eu.habbo.messages.outgoing.rooms;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import gnu.trove.set.hash.THashSet;

import java.util.Objects;

public class UpdateStackHeightComposer extends MessageComposer {
    private int x;
    private int y;
    private short z;
    private double height;

    private THashSet<RoomTile> updateTiles;
    private Room room;

    public UpdateStackHeightComposer(int x, int y, short z, double height) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.height = height;
    }

    public UpdateStackHeightComposer(Room room, THashSet<RoomTile> updateTiles) {
        this.updateTiles = updateTiles;
        this.room = room;
    }

    @Override
    protected ServerMessage composeInternal() {
        //TODO: maybe do this another way? doesn't seem to be very clean but gets the job done
        this.response.init(Outgoing.UpdateStackHeightComposer);
        if (this.updateTiles != null) {
            this.updateTiles.removeIf(Objects::isNull);
            // prevent overflow. Byte max value is 127
            if(this.updateTiles.size() > 127) {
                RoomTile[] tiles = this.updateTiles.toArray(new RoomTile[updateTiles.size()]);
                this.response.appendByte(127);
                for(int i = 0; i < 127; i++) {
                    RoomTile t = tiles[i];
                    updateTiles.remove(t); // remove it from the set
                    this.response.appendByte((int) t.x);
                    this.response.appendByte((int) t.y);
                    if(Emulator.getConfig().getBoolean("custom.stacking.enabled")) {
                        this.response.appendShort((short) (t.z * 256.0));
                    }
                    else {
                        this.response.appendShort(t.relativeHeight());
                    }
                }
                //send the remaining tiles in a new message
                this.room.sendComposer(new UpdateStackHeightComposer(this.room, updateTiles).compose());
                return this.response;
            }

            this.response.appendByte(this.updateTiles.size());
            for (RoomTile t : this.updateTiles) {
                this.response.appendByte((int) t.x);
                this.response.appendByte((int) t.y);
                if(Emulator.getConfig().getBoolean("custom.stacking.enabled")) {
                    this.response.appendShort((short) (t.z * 256.0));
                }
                else {
                    this.response.appendShort(t.relativeHeight());
                }
            }
        } else {
            this.response.appendByte(1);
            this.response.appendByte(this.x);
            this.response.appendByte(this.y);
            if(Emulator.getConfig().getBoolean("custom.stacking.enabled")) {
                this.response.appendShort((short) (this.z * 256.0));
            }
            else {
                this.response.appendShort((int) (this.height));
            }
        }
        return this.response;
    }
}

package com.eu.habbo.core;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class RoomUserPetComposer extends MessageComposer {
    private final int petType;
    private final int race;
    private final String color;
    private final Habbo habbo;

    public RoomUserPetComposer(int petType, int race, String color, Habbo habbo) {
        this.petType = petType;
        this.race = race;
        this.color = color;
        this.habbo = habbo;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.RoomUsersComposer);
        this.response.appendInt(1);
        this.response.appendInt(this.habbo.getHabboInfo().getId());
        this.response.appendString(this.habbo.getHabboInfo().getUsername());
        this.response.appendString("");
        this.response.appendString(this.petType + " " + this.race + " " + this.color + " 2 2 -1 0 3 -1 0");
        this.response.appendInt(this.habbo.getRoomUnit().getId());
        this.response.appendInt(this.habbo.getRoomUnit().getX());
        this.response.appendInt(this.habbo.getRoomUnit().getY());
        this.response.appendString(this.habbo.getRoomUnit().getZ() + "");
        this.response.appendInt(this.habbo.getRoomUnit().getBodyRotation().getValue());
        this.response.appendInt(2);
        this.response.appendInt(this.petType);
        this.response.appendInt(this.habbo.getHabboInfo().getId());
        this.response.appendString(this.habbo.getHabboInfo().getUsername());
        this.response.appendInt(1);
        this.response.appendBoolean(false);
        this.response.appendBoolean(true);
        this.response.appendBoolean(true); //Can toggle breeding permissions.
        this.response.appendBoolean(true);
        this.response.appendBoolean(true); //Can treat?
        this.response.appendBoolean(true); //Can breed
        this.response.appendInt(0);
        this.response.appendString("");
        return this.response;
    }
}

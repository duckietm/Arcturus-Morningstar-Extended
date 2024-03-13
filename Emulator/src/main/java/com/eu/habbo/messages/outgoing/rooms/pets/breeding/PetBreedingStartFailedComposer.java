package com.eu.habbo.messages.outgoing.rooms.pets.breeding;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class PetBreedingStartFailedComposer extends MessageComposer {
    public final static int NO_NESTS = 0;
    public final static int NO_SUITABLE_NESTS = 1;
    public final static int NEST_FULL = 2;
    public final static int NOT_OWNER = 3;
    public final static int ALREADY_IN_NEST = 4;
    public final static int NO_PATH_TO_NEST = 5;
    public final static int TOO_TIRED = 6;

    private final int reason;

    public PetBreedingStartFailedComposer(int reason) {
        this.reason = reason;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.PetBreedingStartFailedComposer);
        this.response.appendInt(this.reason);
        return this.response;
    }
}
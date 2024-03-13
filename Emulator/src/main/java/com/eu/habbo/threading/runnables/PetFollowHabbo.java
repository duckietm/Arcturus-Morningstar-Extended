package com.eu.habbo.threading.runnables;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetTasks;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.users.Habbo;

public class PetFollowHabbo implements Runnable {
    private final int directionOffset;
    private final Habbo habbo;
    private final Pet pet;

    public PetFollowHabbo(Pet pet, Habbo habbo, int offset) {
        this.pet = pet;
        this.habbo = habbo;
        this.directionOffset = offset;
    }

    @Override
    public void run() {
        if (this.pet != null) {
            if (this.pet.getTask() != PetTasks.FOLLOW)
                return;

            if (this.habbo != null) {
                if (this.habbo.getRoomUnit() != null) {
                    if (this.pet.getRoomUnit() != null) {
                        RoomTile target = this.habbo.getHabboInfo().getCurrentRoom().getLayout().getTileInFront(this.habbo.getRoomUnit().getCurrentLocation(), Math.abs((this.habbo.getRoomUnit().getBodyRotation().getValue() + this.directionOffset + 4) % 8));

                        if (target != null) {
                            if (target.x < 0 || target.y < 0)
                                target = this.habbo.getHabboInfo().getCurrentRoom().getLayout().getTileInFront(this.habbo.getRoomUnit().getCurrentLocation(), this.habbo.getRoomUnit().getBodyRotation().getValue());

                            if (target.x >= 0 && target.y >= 0) {
                                if (this.pet.getRoom().getLayout().tileWalkable(target.x, target.y)) {
                                    this.pet.getRoomUnit().setGoalLocation(target);
                                    this.pet.getRoomUnit().setCanWalk(true);
                                    this.pet.setTask(PetTasks.FOLLOW);
                                }
                            }
                            Emulator.getThreading().run(this, 500);
                        }
                    }
                }
            }
        }
    }
}

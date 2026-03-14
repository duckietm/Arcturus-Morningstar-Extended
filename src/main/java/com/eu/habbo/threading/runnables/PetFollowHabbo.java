package com.eu.habbo.threading.runnables;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetTasks;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
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
        // Comprehensive null checks
        if (this.pet == null || this.pet.getRoom() == null || this.pet.getRoomUnit() == null) {
            return; // Stop following - pet or room is gone
        }
        
        // Check if task is any follow type
        PetTasks task = this.pet.getTask();
        if (task != PetTasks.FOLLOW && task != PetTasks.FOLLOW_LEFT && task != PetTasks.FOLLOW_RIGHT) {
            return; // Task was changed, stop
        }
        
        // Check if habbo is still valid
        if (this.habbo == null || this.habbo.getRoomUnit() == null) {
            this.pet.setTask(PetTasks.FREE);
            return; // Owner gone, stop following
        }
        
        // Check if habbo is still in the same room as the pet
        if (this.habbo.getHabboInfo().getCurrentRoom() != this.pet.getRoom()) {
            this.pet.setTask(PetTasks.FREE);
            this.pet.say(this.pet.getPetData().randomVocal(PetVocalsType.GENERIC_SAD));
            return;
        }
        
        // Calculate target position
        RoomTile habboTile = this.habbo.getRoomUnit().getCurrentLocation();
        if (habboTile == null) {
            Emulator.getThreading().run(this, 500);
            return;
        }
        
        int targetRotation = Math.abs((this.habbo.getRoomUnit().getBodyRotation().getValue() 
            + this.directionOffset + 4) % 8);
        
        RoomTile target = this.pet.getRoom().getLayout().getTileInFront(habboTile, targetRotation);
        
        // Validate target tile - try alternative positions if needed
        if (target == null || target.x < 0 || target.y < 0 
            || !this.pet.getRoom().getLayout().tileWalkable(target.x, target.y)) {
            // Try directly behind habbo
            target = this.pet.getRoom().getLayout().getTileInFront(
                habboTile, 
                (this.habbo.getRoomUnit().getBodyRotation().getValue() + 4) % 8
            );
        }
        
        // Try other adjacent positions if still invalid
        if (target == null || target.x < 0 || target.y < 0 
            || !this.pet.getRoom().getLayout().tileWalkable(target.x, target.y)) {
            // Try to the left
            target = this.pet.getRoom().getLayout().getTileInFront(
                habboTile, 
                (this.habbo.getRoomUnit().getBodyRotation().getValue() + 2) % 8
            );
        }
        
        if (target == null || target.x < 0 || target.y < 0 
            || !this.pet.getRoom().getLayout().tileWalkable(target.x, target.y)) {
            // Try to the right
            target = this.pet.getRoom().getLayout().getTileInFront(
                habboTile, 
                (this.habbo.getRoomUnit().getBodyRotation().getValue() + 6) % 8
            );
        }
        
        // If we found a valid target, move there
        if (target != null && target.x >= 0 && target.y >= 0) {
            if (this.pet.getRoom().getLayout().tileWalkable(target.x, target.y)) {
                this.pet.getRoomUnit().setGoalLocation(target);
                this.pet.getRoomUnit().setCanWalk(true);
            }
        }
        
        // Continue following with slight randomization for natural behavior
        int nextDelay = 400 + Emulator.getRandom().nextInt(200);
        Emulator.getThreading().run(this, nextDelay);
    }
}

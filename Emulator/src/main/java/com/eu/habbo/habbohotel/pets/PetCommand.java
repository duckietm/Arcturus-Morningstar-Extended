package com.eu.habbo.habbohotel.pets;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.users.Habbo;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PetCommand implements Comparable<PetCommand> {

    public final int id;


    public final String key;


    public final int level;


    public final int xp;


    public final int energyCost;


    public final int happynessCost;


    public final PetAction action;

    public PetCommand(ResultSet set, PetAction action) throws SQLException {
        this.id = set.getInt("command_id");
        this.key = set.getString("text");
        this.level = set.getInt("required_level");
        this.xp = set.getInt("reward_xp");
        this.energyCost = set.getInt("cost_energy");
        this.happynessCost = set.getInt("cost_happyness");
        this.action = action;
    }

    @Override
    public int compareTo(PetCommand o) {
        return this.level - o.level;
    }

    public void handle(Pet pet, Habbo habbo, String[] data) {
        if (Emulator.getRandom().nextInt((pet.level - this.level <= 0 ? 2 : pet.level - this.level) + 2) == 0) {
            pet.say(pet.petData.randomVocal(PetVocalsType.DISOBEY));
            return;
        }

        if (this.action != null) {
            if (this.action.petTask != pet.getTask()) {
                if (this.action.stopsPetWalking) {
                    pet.getRoomUnit().setGoalLocation(pet.getRoomUnit().getCurrentLocation());
                }
                if (this.action.apply(pet, habbo, data)) {
                    for (RoomUnitStatus status : this.action.statusToRemove) {
                        pet.getRoomUnit().removeStatus(status);
                    }

                    for (RoomUnitStatus status : this.action.statusToSet) {
                        pet.getRoomUnit().setStatus(status, "0");
                    }

                    pet.getRoomUnit().setStatus(RoomUnitStatus.GESTURE, this.action.gestureToSet);

                    pet.addEnergy(-this.energyCost);
                    pet.addHappyness(-this.happynessCost);
                    pet.addExperience(this.xp);
                }
            }
        }
    }
}
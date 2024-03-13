package com.eu.habbo.habbohotel.items.interactions.pets;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.achievements.AchievementManager;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionDefault;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetTasks;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.rooms.RoomUserRotation;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserStatusComposer;
import com.eu.habbo.threading.runnables.PetClearPosture;

import java.sql.ResultSet;
import java.sql.SQLException;

public class InteractionPetDrink extends InteractionDefault {
    public InteractionPetDrink(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public InteractionPetDrink(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public void onWalkOn(RoomUnit roomUnit, Room room, Object[] objects) throws Exception {
        super.onWalkOn(roomUnit, room, objects);

        Pet pet = room.getPet(roomUnit);

        if (pet != null) {
            if (pet.getPetData().haveDrinkItem(this)) {
                if (pet.levelThirst >= 35) {
                    pet.setTask(PetTasks.EAT);
                    pet.getRoomUnit().setGoalLocation(room.getLayout().getTile(this.getX(), this.getY()));
                    pet.getRoomUnit().setRotation(RoomUserRotation.values()[this.getRotation()]);
                    pet.getRoomUnit().clearStatus();
                    pet.getRoomUnit().removeStatus(RoomUnitStatus.MOVE);
                    pet.getRoomUnit().setStatus(RoomUnitStatus.EAT, "0");
                    pet.addThirst(-75);
                    room.sendComposer(new RoomUserStatusComposer(roomUnit).compose());
                    Emulator.getThreading().run(new PetClearPosture(pet, RoomUnitStatus.EAT, null, true), 500);

                    AchievementManager.progressAchievement(Emulator.getGameEnvironment().getHabboManager().getHabbo(pet.getUserId()), Emulator.getGameEnvironment().getAchievementManager().getAchievement("PetFeeding"), 75);
                }
            }
        }
    }

    @Override
    public boolean allowWiredResetState() {
        return false;
    }
}

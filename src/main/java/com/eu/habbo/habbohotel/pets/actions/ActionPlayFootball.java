package com.eu.habbo.habbohotel.pets.actions;

import com.eu.habbo.habbohotel.items.interactions.InteractionPushable;
import com.eu.habbo.habbohotel.items.interactions.games.football.InteractionFootball;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetAction;
import com.eu.habbo.habbohotel.pets.PetTasks;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.outgoing.generic.alerts.GenericAlertComposer;

public class ActionPlayFootball extends PetAction {
    public ActionPlayFootball() {
        super(PetTasks.PLAY_FOOTBALL, false);
    }

    @Override
    public boolean apply(Pet pet, Habbo habbo, String[] data) {
        Room room = pet.getRoom();

        if (room == null || room.getLayout() == null) {
            return false;
        }

        if (pet.getRoomUnit() == null) {
            return false;
        }

        if (pet.getTask() != PetTasks.PLAY_FOOTBALL
                && Pet.GLOBAL_FOOTBALL_PET_COUNT.get() >= InteractionFootball.MAX_FOOTBALL_PETS) {
            if (habbo != null && habbo.getClient() != null) {
                habbo.getClient().sendResponse(new GenericAlertComposer(
                        "Sorry, you already have 5 pets playing football in the rooms"));
            }
            return false;
        }

        HabboItem nearestBall = null;
        double nearestDistance = Double.MAX_VALUE;
        RoomTile petTile = pet.getRoomUnit().getCurrentLocation();

        if (petTile == null) {
            return false;
        }

        for (HabboItem item : room.getFloorItems()) {
            if (item instanceof InteractionPushable) {
                RoomTile ballTile = room.getLayout().getTile(item.getX(), item.getY());
                if (ballTile != null) {
                    double distance = petTile.distance(ballTile);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearestBall = item;
                    }
                }
            }
        }

        if (nearestBall == null) {
            pet.say(pet.getPetData().randomVocal(PetVocalsType.DISOBEY));
            return false;
        }

        pet.setTask(PetTasks.PLAY_FOOTBALL);
        pet.getRoomUnit().setCanWalk(true);
        pet.getRoomUnit().setGoalLocation(room.getLayout().getTile(nearestBall.getX(), nearestBall.getY()));

        if (pet.getHappiness() > 75) {
            pet.say(pet.getPetData().randomVocal(PetVocalsType.PLAYFUL));
        } else {
            pet.say(pet.getPetData().randomVocal(PetVocalsType.GENERIC_NEUTRAL));
        }

        return true;
    }
}

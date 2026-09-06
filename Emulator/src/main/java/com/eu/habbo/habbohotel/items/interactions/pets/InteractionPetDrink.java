package com.eu.habbo.habbohotel.items.interactions.pets;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.achievements.AchievementManager;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionDefault;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
import com.eu.habbo.habbohotel.pets.RideablePet;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomLayout;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.rooms.RoomUserRotation;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.threading.runnables.PetClearPosture;
import com.eu.habbo.threading.runnables.RoomUnitWalkToLocation;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pet water bowl (items_base.interaction_type = pet_drink).
 *
 * <p>Water level convention, taken from the furni bundles (waterbowl, water_bowl1, waterbowl_basic): the water layer
 * has one frame per state and the frame grows with the state, so <b>state 0 is an empty bowl and the last state is a
 * full one</b>. A user double-click fills the bowl (last state), every drink lowers it by one state, and pets only
 * drink while the level is above 0.
 */
public class InteractionPetDrink extends InteractionDefault {
    private static final Logger LOGGER = LoggerFactory.getLogger(InteractionPetDrink.class);

    public InteractionPetDrink(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public InteractionPetDrink(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    /** Current water level (0 = empty). Malformed extradata counts as empty. */
    public int getWaterLevel() {
        String extradata = this.getExtradata();
        if (extradata == null || extradata.isBlank()) return 0;

        try {
            return Math.max(0, Integer.parseInt(extradata.trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** True while there is water left for a pet to drink. */
    public boolean hasWater() {
        return this.getWaterLevel() > 0;
    }

    @Override
    public boolean canToggle(Habbo habbo, Room room) {
        return RoomLayout.tilesAdjecent(
                room.getLayout().getTile(this.getX(), this.getY()),
                habbo.getRoomUnit().getCurrentLocation());
    }

    @Override
    public void onClick(GameClient client, Room room, Object[] objects) throws Exception {
        if (client == null) return;

        if (!this.canToggle(client.getHabbo(), room)) {
            RoomTile closestTile = null;
            for (RoomTile tile :
                    room.getLayout().getTilesAround(room.getLayout().getTile(this.getX(), this.getY()))) {
                if (tile.isWalkable()
                        && (closestTile == null
                                || closestTile.distance(
                                                client.getHabbo().getRoomUnit().getCurrentLocation())
                                        > tile.distance(
                                                client.getHabbo().getRoomUnit().getCurrentLocation()))) {
                    closestTile = tile;
                }
            }

            if (closestTile != null
                    && !closestTile.equals(client.getHabbo().getRoomUnit().getCurrentLocation())) {
                List<Runnable> onSuccess = new ArrayList<>();
                onSuccess.add(() -> this.fill(room));

                client.getHabbo().getRoomUnit().setGoalLocation(closestTile);
                Emulator.getThreading()
                        .run(new RoomUnitWalkToLocation(
                                client.getHabbo().getRoomUnit(), closestTile, room, onSuccess, new ArrayList<>()));
            }

            return;
        }

        // Adjacent already: fill the bowl right away (InteractionDefault would only cycle the sprite state).
        this.fill(room);
    }

    @Override
    public void onWalkOn(RoomUnit roomUnit, Room room, Object[] objects) throws Exception {
        super.onWalkOn(roomUnit, room, objects);

        if (this.getExtradata() == null || this.getExtradata().isEmpty()) this.setExtradata("0");

        // Empty bowl: nothing to drink until someone refills it.
        if (!this.hasWater()) {
            return;
        }

        Pet pet = room.getPet(roomUnit);

        if (pet != null
                && !(pet instanceof RideablePet && ((RideablePet) pet).getRider() != null)
                && pet.getPetData().haveDrinkItem(this)
                && pet.levelThirst >= 35) {
            pet.clearPosture();
            pet.getRoomUnit().setGoalLocation(room.getLayout().getTile(this.getX(), this.getY()));
            pet.getRoomUnit().setRotation(RoomUserRotation.values()[this.getRotation()]);
            pet.getRoomUnit().clearStatus();
            pet.getRoomUnit()
                    .setStatus(
                            RoomUnitStatus.EAT,
                            pet.getRoomUnit().getCurrentLocation().getStackHeight() + "");
            pet.packetUpdate = true;

            pet.say(pet.getPetData().randomVocal(PetVocalsType.DRINKING));

            Emulator.getThreading()
                    .run(
                            () -> {
                                pet.addThirst(-75);
                                // One drink lowers the water level by one state.
                                this.change(room, -1);
                                pet.getRoomUnit().clearStatus();
                                Emulator.getThreading()
                                        .run(new PetClearPosture(pet, RoomUnitStatus.EAT, null, true), 0);
                                pet.packetUpdate = true;
                            },
                            500);

            AchievementManager.progressAchievement(
                    Emulator.getGameEnvironment().getHabboManager().getHabbo(pet.getUserId()),
                    Emulator.getGameEnvironment().getAchievementManager().getAchievement("PetFeeding"),
                    75);
        }
    }

    @Override
    public boolean allowWiredResetState() {
        return false;
    }

    /** Fills the bowl to its last (full) state. */
    private void fill(Room room) {
        this.change(room, this.getBaseItem().getStateCount() - 1 - this.getWaterLevel());
    }

    private void change(Room room, int amount) {
        int state = this.getWaterLevel() + amount;
        int maxState = Math.max(0, this.getBaseItem().getStateCount() - 1);

        if (state > maxState) state = maxState;
        if (state < 0) state = 0;

        this.setExtradata(state + "");
        this.needsUpdate(true);
        room.updateItemState(this);
    }
}

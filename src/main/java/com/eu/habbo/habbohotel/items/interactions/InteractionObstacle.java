package com.eu.habbo.habbohotel.items.interactions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.achievements.AchievementManager;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.pets.HorsePet;
import com.eu.habbo.habbohotel.rooms.*;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.threading.runnables.HabboItemNewState;

import java.sql.ResultSet;
import java.sql.SQLException;

public class InteractionObstacle extends HabboItem {

    public InteractionObstacle(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        this.setExtradata("0");
    }

    public InteractionObstacle(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
        this.setExtradata("0");
    }

    @Override
    public void serializeExtradata(ServerMessage serverMessage) {
        serverMessage.appendInt((this.isLimited() ? 256 : 0));
        serverMessage.appendString(this.getExtradata());

        super.serializeExtradata(serverMessage);
    }

    @Override
    public boolean canWalkOn(RoomUnit roomUnit, Room room, Object[] objects) {
        return true;
    }

    @Override
    public boolean isWalkable() {
        return true;
    }

    @Override
    public void onClick(GameClient client, Room room, Object[] objects) throws Exception {
        super.onClick(client, room, objects);
    }

    @Override
    public void onWalk(RoomUnit roomUnit, Room room, Object[] objects) throws Exception {
        final HorsePet horse = room.getHabboHorse(roomUnit);

        if (horse == null) {
            return;
        }

        if (horse.getRoomUnit().hasStatus(RoomUnitStatus.JUMP)) {
            return;
        }

        // Random state.
        int state = 0;
        for (int i = 0; i < 2; i++) {
            state = Emulator.getRandom().nextInt(4) + 1;

            if (state == 4)
                break;
        }

        this.setExtradata(state + "");

        // Reset state.
        Emulator.getThreading().run(new HabboItemNewState(this, room, "0"), 2000);

        // Jump animation.
        roomUnit.removeStatus(RoomUnitStatus.MOVE);
        roomUnit.removeStatus(RoomUnitStatus.JUMP);
        roomUnit.statusUpdate(true);

        horse.getRoomUnit().removeStatus(RoomUnitStatus.JUMP);
        horse.getRoomUnit().removeStatus(RoomUnitStatus.MOVE);
        horse.getRoomUnit().setStatus(RoomUnitStatus.JUMP, "0");
        room.updateRoomUnit(horse.getRoomUnit());

        Emulator.getThreading().run(() -> {
            horse.getRoomUnit().removeStatus(RoomUnitStatus.JUMP);
            room.updateRoomUnit(horse.getRoomUnit());
        }, 1000);

        // Achievement.
        final Habbo habbo = horse.getRider();

        AchievementManager.progressAchievement(habbo, Emulator.getGameEnvironment().getAchievementManager().getAchievement("HorseConsecutiveJumpsCount"));
        AchievementManager.progressAchievement(habbo, Emulator.getGameEnvironment().getAchievementManager().getAchievement("HorseJumping"));

        room.updateItemState(this);
    }

    @Override
    public void onWalkOn(RoomUnit roomUnit, Room room, Object[] objects) throws Exception {
        super.onWalkOn(roomUnit, room, objects);

        final HorsePet horse = room.getHabboHorse(roomUnit);

        if (horse == null) {
            return;
        }

        if (roomUnit.getBodyRotation().getValue() % 2 != 0) {
            return;
        }

        if (this.getRotation() == 2) {
            if (roomUnit.getBodyRotation() == RoomUserRotation.WEST) {
                roomUnit.setGoalLocation(room.getLayout().getTile((short) (roomUnit.getX() - 3), roomUnit.getY()));
            } else if (roomUnit.getBodyRotation() == RoomUserRotation.EAST) {
                roomUnit.setGoalLocation(room.getLayout().getTile((short) (roomUnit.getX() + 3), roomUnit.getY()));
            }
        } else if (this.getRotation() == 4) {
            if (roomUnit.getBodyRotation() == RoomUserRotation.NORTH) {
                roomUnit.setGoalLocation(room.getLayout().getTile(roomUnit.getX(), (short) (roomUnit.getY() - 3)));
            } else if (roomUnit.getBodyRotation() == RoomUserRotation.SOUTH) {
                roomUnit.setGoalLocation(room.getLayout().getTile(roomUnit.getX(), (short) (roomUnit.getY() + 3)));
            }
        }
    }

    @Override
    public void onWalkOff(RoomUnit roomUnit, Room room, Object[] objects) throws Exception {
        super.onWalkOff(roomUnit, room, objects);

        final HorsePet horse = room.getHabboHorse(roomUnit);

        if (horse == null) {
            return;
        }

         horse.getRoomUnit().removeStatus(RoomUnitStatus.JUMP);
    }
}
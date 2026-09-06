package com.eu.habbo.habbohotel.items.interactions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.users.clothingvalidation.ClothingValidationManager;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserDataComposer;
import com.eu.habbo.messages.outgoing.users.UserDataComposer;

import java.sql.ResultSet;
import java.sql.SQLException;

public class InteractionMannequin extends HabboItem {
    private static final String[] CLOTHING_PART_TYPES = {"ch", "cc", "lg", "sh", "wa", "ca"};

    public InteractionMannequin(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public InteractionMannequin(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public int getMaximumRotations() {
        return 8;
    }

    @Override
    public void serializeExtradata(ServerMessage serverMessage) {
        serverMessage.appendInt(1 + (this.isLimited() ? 256 : 0));
        serverMessage.appendInt(3);
        String[] data = this.getExtradata().split(":", 3);
        if (data.length >= 2) {
            serverMessage.appendString("GENDER");
            serverMessage.appendString(data[0].toLowerCase());
            serverMessage.appendString("FIGURE");
            serverMessage.appendString(data[1]);
            serverMessage.appendString("OUTFIT_NAME");
            serverMessage.appendString((data.length >= 3 ? data[2] : ""));
        } else {
            serverMessage.appendString("GENDER");
            serverMessage.appendString("m");
            serverMessage.appendString("FIGURE");
            serverMessage.appendString("");
            serverMessage.appendString("OUTFIT_NAME");
            serverMessage.appendString("My Look");
            this.setExtradata("m::My look");
            this.needsUpdate(true);
            Emulator.getThreading().run(this);
        }
        super.serializeExtradata(serverMessage);
    }

    @Override
    public boolean canWalkOn(RoomUnit roomUnit, Room room, Object[] objects) {
        return true;
    }

    @Override
    public boolean isWalkable() {
        return false;
    }

    @Override
    public void onClick(GameClient client, Room room, Object[] objects) throws Exception {
        String[] data = this.getExtradata().split(":", 3);

        if(data.length < 2)
            return;

        String gender = data[0];
        String figure = data[1];

        if (figure.isEmpty()) return;

        // This hotel lets anyone wear a mannequin's outfit (staff looks are usually saved by one
        // gender only); set hotel.mannequin.require_gender=true for the official behaviour.
        if (Emulator.getConfig().getBoolean("hotel.mannequin.require_gender", false)
                && (gender.isEmpty()
                        || (!gender.equalsIgnoreCase("m") && !gender.equalsIgnoreCase("f"))
                        || !client.getHabbo().getHabboInfo().getGender().name().equalsIgnoreCase(gender))) {
            return;
        }

        StringBuilder newFigure = new StringBuilder();

        for (String playerFigurePart : client.getHabbo().getHabboInfo().getLook().split("\\.")) {
            if (!isClothingPart(playerFigurePart))
                newFigure.append(playerFigurePart).append('.');
        }

        StringBuilder newFigureParts = new StringBuilder();
        for (String newFigurePart : figure.split("\\.")) {
            if (isClothingPart(newFigurePart)) {
                if (newFigureParts.length() > 0) newFigureParts.append('.');
                newFigureParts.append(newFigurePart);
            }
        }

        if (newFigureParts.length() == 0) return;

        String newLook = newFigure.append(newFigureParts).toString();

        if (newLook.length() > 512)
            return;

        client.getHabbo().getHabboInfo().setLook(ClothingValidationManager.VALIDATE_ON_MANNEQUIN ? ClothingValidationManager.validateLook(client.getHabbo(), newLook, client.getHabbo().getHabboInfo().getGender().name()) : newLook);
        room.sendComposer(new RoomUserDataComposer(client.getHabbo()).compose());
        client.sendResponse(new UserDataComposer(client.getHabbo()));
    }

    private static boolean isClothingPart(String figurePart) {
        for (String type : CLOTHING_PART_TYPES) {
            if (figurePart.startsWith(type + "-")) return true;
        }
        return false;
    }

    @Override
    public void onWalk(RoomUnit roomUnit, Room room, Object[] objects) throws Exception {

    }

    @Override
    public void onWalkOn(RoomUnit roomUnit, Room room, Object[] objects) throws Exception {

    }

    @Override
    public void onWalkOff(RoomUnit roomUnit, Room room, Object[] objects) throws Exception {

    }
}

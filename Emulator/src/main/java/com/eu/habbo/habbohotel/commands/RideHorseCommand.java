package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.pets.HorsePet;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.RideablePet;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.threading.runnables.RoomUnitRidePet;

import java.util.List;

/**
 * {@code :cavalca <nome>} — mount the horse with that name in the current room, saddle or not.
 * It must be a horse ({@link HorsePet}), free, and yours or "anyone can ride" (staff bypasses).
 */
public class RideHorseCommand extends Command {
    public RideHorseCommand() {
        super(null, Emulator.getTexts().getValue("commands.keys.cmd_cavalca", "cavalca;ride;monta").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        Habbo habbo = gameClient.getHabbo();
        Room room = habbo.getHabboInfo().getCurrentRoom();
        if (room == null) return true;

        if (params.length < 2) {
            habbo.whisper(Emulator.getTexts().getValue("commands.description.cmd_cavalca", ":cavalca <nome>"), RoomChatMessageBubbles.ALERT);
            return true;
        }

        // already riding: the command dismounts
        if (habbo.getHabboInfo().getRiding() != null) {
            habbo.getHabboInfo().dismountPet();
            return true;
        }

        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 1; i < params.length; i++) {
            if (i > 1) nameBuilder.append(' ');
            nameBuilder.append(params[i]);
        }
        String name = nameBuilder.toString().trim();

        Pet found = null;
        for (Pet pet : room.getCurrentPets().values()) {
            if (pet != null && pet.getName() != null && pet.getName().equalsIgnoreCase(name)) {
                found = pet;
                break;
            }
        }

        if (found == null) {
            habbo.whisper(text("cavalca.error.notfound", "Nessun cavallo chiamato %name% in questa stanza.", name), RoomChatMessageBubbles.ALERT);
            return true;
        }

        if (!(found instanceof HorsePet)) {
            habbo.whisper(text("cavalca.error.nothorse", "%name% non è un cavallo!", name), RoomChatMessageBubbles.ALERT);
            return true;
        }

        RideablePet horse = (RideablePet) found;

        if (horse.getRider() != null) {
            habbo.whisper(text("cavalca.error.busy", "%name% ha già qualcuno in sella.", name), RoomChatMessageBubbles.ALERT);
            return true;
        }

        boolean allowed = horse.anyoneCanRide()
                || habbo.getHabboInfo().getId() == horse.getUserId()
                || HotelNotificationCommand.hasStaffRank(habbo);
        if (!allowed) {
            habbo.whisper(text("cavalca.error.owner", "Solo il proprietario può cavalcare %name%.", name), RoomChatMessageBubbles.ALERT);
            return true;
        }

        // no saddle required: same mount routine the ride button uses, minus the client-side saddle gate
        List<RoomTile> availableTiles = room.getLayout().getWalkableTilesAround(horse.getRoomUnit().getCurrentLocation());
        if (availableTiles.isEmpty()) {
            habbo.whisper(text("cavalca.error.notfound", "Nessun cavallo chiamato %name% in questa stanza.", name), RoomChatMessageBubbles.ALERT);
            return true;
        }

        RoomTile goalTile = availableTiles.get(0);
        habbo.getRoomUnit().setGoalLocation(goalTile);
        Emulator.getThreading().run(new RoomUnitRidePet(horse, habbo, goalTile));
        horse.getRoomUnit().setWalkTimeOut(3 + Emulator.getIntUnixTimestamp());
        horse.getRoomUnit().stopWalking();

        habbo.whisper(text("cavalca.ok", "Sali in sella a %name%!", name), RoomChatMessageBubbles.ALERT);
        return true;
    }

    private static String text(String key, String fallback, String name) {
        return Emulator.getTexts().getValue(key, fallback).replace("%name%", name);
    }
}

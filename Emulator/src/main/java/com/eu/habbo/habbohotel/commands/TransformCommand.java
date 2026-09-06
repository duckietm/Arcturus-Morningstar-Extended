package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.core.RoomUserPetComposer;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.pets.PetData;
import com.eu.habbo.habbohotel.pets.PetManager;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.rooms.RoomUnitType;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.rooms.pets.PetMorphListComposer;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserRemoveComposer;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUsersComposer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * {@code :transform} / {@code :pet}. Without arguments the client receives the pet list
 * ({@link PetMorphListComposer}) and opens the morph picker; with arguments the pet is
 * chosen by numeric type id or by (multi-word) name, optionally followed by a breed index
 * and a hex colour. {@code :transform habbo} restores the avatar.
 */
public class TransformCommand extends Command {
    private static final java.util.Set<String> RESET_WORDS = java.util.Set.of("habbo", "off", "normale", "reset", "umano", "human");

    protected TransformCommand() {
        super("cmd_transform", Emulator.getTexts().getValue("commands.keys.cmd_transform").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        Habbo habbo = gameClient.getHabbo();
        Room room = habbo.getHabboInfo().getCurrentRoom();
        if (room == null) return true;

        if (params.length >= 2 && RESET_WORDS.contains(params[1].toLowerCase(Locale.ROOT))) {
            restore(habbo, room);
            return true;
        }

        PetManager petManager = Emulator.getGameEnvironment().getPetManager();

        if (params.length == 1) {
            gameClient.sendResponse(new PetMorphListComposer(habbo));
            return true;
        }

        List<String> tokens = new ArrayList<>(Arrays.asList(params).subList(1, params.length));
        String color = "FFFFFF";
        int race = 0;

        if (tokens.size() >= 2 && tokens.get(tokens.size() - 1).matches("(?i)[0-9a-f]{6}")
                && !tokens.get(tokens.size() - 1).matches("[0-9]{1,3}")) {
            color = tokens.remove(tokens.size() - 1).toUpperCase(Locale.ROOT);
        }
        if (tokens.size() >= 2 && tokens.get(tokens.size() - 1).matches("[0-9]{1,3}")) {
            race = Integer.parseInt(tokens.remove(tokens.size() - 1));
        }

        String wanted = String.join(" ", tokens).trim();
        PetData petData = resolvePet(petManager, wanted);

        if (petData == null) {
            habbo.whisper(Emulator.getTexts().getValue("commands.generic.cmd_transform.not_found", "Animale non trovato: %name%").replace("%name%", wanted), RoomChatMessageBubbles.ALERT);
            gameClient.sendResponse(new PetMorphListComposer(habbo));
            return true;
        }

        int races = petManager.getRaceCount(petData.getType());
        if (race < 0 || (races > 0 && race >= races)) race = 0;

        RoomUnit roomUnit = habbo.getRoomUnit();
        roomUnit.setRoomUnitType(RoomUnitType.PET);
        habbo.getHabboStats().cache.put("pet_type", petData);
        habbo.getHabboStats().cache.put("pet_race", race);
        habbo.getHabboStats().cache.put("pet_color", color);
        room.sendComposer(new RoomUserRemoveComposer(roomUnit).compose());
        room.sendComposer(new RoomUserPetComposer(petData.getType(), race, color, habbo).compose());
        return true;
    }

    private static void restore(Habbo habbo, Room room) {
        RoomUnit roomUnit = habbo.getRoomUnit();
        if (roomUnit.getRoomUnitType() != RoomUnitType.PET) return;

        roomUnit.setRoomUnitType(RoomUnitType.USER);
        habbo.getHabboStats().cache.remove("pet_type");
        habbo.getHabboStats().cache.remove("pet_race");
        habbo.getHabboStats().cache.remove("pet_color");
        room.sendComposer(new RoomUserRemoveComposer(roomUnit).compose());
        room.sendComposer(new RoomUsersComposer(habbo).compose());
    }

    /** Numeric type id, exact name, normalised name, then a unique name prefix. */
    static PetData resolvePet(PetManager petManager, String wanted) {
        if (wanted == null || wanted.isBlank()) return null;

        if (wanted.matches("[0-9]{1,3}")) {
            int type = Integer.parseInt(wanted);
            for (PetData data : petManager.getPetData()) {
                if (data.getType() == type) return data;
            }
            return null;
        }

        PetData exact = petManager.getPetData(wanted);
        if (exact != null) return exact;

        String normalizedWanted = normalize(wanted);
        if (normalizedWanted.isEmpty()) return null;

        PetData prefixMatch = null;
        int prefixMatches = 0;
        for (PetData data : petManager.getPetData()) {
            String normalizedName = normalize(data.getName());
            if (normalizedName.equals(normalizedWanted)) return data;
            // "Guinea Pig / Cavia" style names: match either side of the slash too
            for (String part : data.getName().split("/")) {
                if (normalize(part).equals(normalizedWanted)) return data;
            }
            if (normalizedName.startsWith(normalizedWanted)) {
                prefixMatch = data;
                prefixMatches++;
            }
        }
        return prefixMatches == 1 ? prefixMatch : null;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}

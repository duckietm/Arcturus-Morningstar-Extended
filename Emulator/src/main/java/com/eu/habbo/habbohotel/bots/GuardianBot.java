package com.eu.habbo.habbohotel.bots;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessage;
import com.eu.habbo.habbohotel.users.Habbo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "Guardiano": the room bouncer. Greets people who walk in, warns anyone who insults (configurable
 * word list in {@code hotel.bot.guardian.words}, plus whatever the hotel wordfilter replaced) and
 * kicks them after {@code hotel.bot.guardian.strikes} warnings. The room owner, users with rights,
 * the bot owner and unkickable staff are never touched.
 */
public class GuardianBot extends Bot {
    public static final String BOT_TYPE = "guardian";

    private final Map<Integer, Integer> strikes = new ConcurrentHashMap<>();
    private final Map<Integer, Long> lastGreeting = new ConcurrentHashMap<>();

    public GuardianBot(ResultSet set) throws SQLException {
        super(set);
    }

    public GuardianBot(Bot bot) {
        super(bot);
    }

    public static void initialise() {
    }

    public static void dispose() {
    }

    public void onUserEnter(Habbo habbo) {
        Room room = this.getRoom();
        if (habbo == null || room == null) return;
        if (!Emulator.getConfig().getBoolean("hotel.bot.guardian.greet", true)) return;

        long now = System.currentTimeMillis();
        Long last = this.lastGreeting.get(habbo.getHabboInfo().getId());
        if (last != null && now - last < 60_000L) return;
        this.lastGreeting.put(habbo.getHabboInfo().getId(), now);

        this.lookAt(habbo);
        this.talk(Emulator.getTexts()
                .getValue("bots.guardian.welcome", "Benvenuto %username%! Qui comando io: niente insulti o ti butto fuori.")
                .replace("%username%", habbo.getHabboInfo().getUsername()));
    }

    @Override
    public void onUserSay(final RoomChatMessage message) {
        Room room = this.getRoom();
        Habbo habbo = message.getHabbo();
        if (room == null || habbo == null || habbo.getRoomUnit() == null) return;
        if (this.isExempt(room, habbo)) return;

        String raw = message.getUnfilteredMessage() != null ? message.getUnfilteredMessage() : message.getMessage();
        if (raw == null || raw.trim().isEmpty()) return;

        boolean offending = message.getMessage() != null && !message.getMessage().equals(raw);
        if (!offending) offending = containsBannedWord(raw);
        if (!offending) return;

        int max = Math.max(1, Emulator.getConfig().getInt("hotel.bot.guardian.strikes", 2));
        int count = this.strikes.merge(habbo.getHabboInfo().getId(), 1, Integer::sum);

        this.lookAt(habbo);

        if (count >= max) {
            this.strikes.remove(habbo.getHabboInfo().getId());
            this.talk(Emulator.getTexts()
                    .getValue("bots.guardian.kick", "%username%, fuori di qui! Te l'avevo detto.")
                    .replace("%username%", habbo.getHabboInfo().getUsername()));
            Emulator.getThreading().run(() -> {
                if (habbo.getHabboInfo().getCurrentRoom() == room) room.kickHabbo(habbo, true);
            }, 1500);
            return;
        }

        this.talk(Emulator.getTexts()
                .getValue("bots.guardian.warn", "%username%, moderati il linguaggio! Avvertimento %strike%/%max%.")
                .replace("%username%", habbo.getHabboInfo().getUsername())
                .replace("%strike%", String.valueOf(count))
                .replace("%max%", String.valueOf(max)));
    }

    private boolean isExempt(Room room, Habbo habbo) {
        if (room.isOwner(habbo) || room.hasRights(habbo)) return true;
        if (habbo.getHabboInfo().getId() == this.getOwnerId()) return true;
        return habbo.hasPermission(Permission.ACC_UNKICKABLE) || habbo.hasPermission(Permission.ACC_ANYROOMOWNER);
    }

    private static String fold(String value) {
        return Normalizer.normalize(value.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private static boolean containsBannedWord(String message) {
        String folded = " " + fold(message).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ") + " ";
        for (String word : bannedWords()) {
            if (folded.contains(" " + word + " ")) return true;
        }
        return false;
    }

    private static List<String> bannedWords() {
        List<String> words = new ArrayList<>();
        String configured = Emulator.getConfig().getValue("hotel.bot.guardian.words", "");
        for (String word : configured.split(";")) {
            String folded = fold(word.trim());
            if (!folded.isEmpty()) words.add(folded);
        }
        return words;
    }
}

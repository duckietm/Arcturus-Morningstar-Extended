package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.generic.alerts.BubbleAlertComposer;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * {@code :notifica <arb|staff|pok|evento> [testo]} — hotel-wide notification with the current room's
 * name/description and a clickable link that takes the reader to the room.
 *
 * <p>Every template lives in {@code emulator_texts} (editable from housekeeping):
 * {@code notifica.<tipo>.title}, {@code .message}, {@code .image}, {@code .link_title}, {@code .display}.
 * Placeholders: {@code %user%}, {@code %room%}, {@code %roomid%}, {@code %desc%}, {@code %owner%},
 * {@code %extra%}.
 */
public class HotelNotificationCommand extends Command {
    private static final String[] TYPES = {"arb", "staff", "pok", "evento"};

    public HotelNotificationCommand() {
        super(null, Emulator.getTexts().getValue("commands.keys.cmd_notifica", "notifica;notify;annuncia").split(";"));
    }

    static boolean hasStaffRank(Habbo habbo) {
        int minRank = Integer.parseInt(Emulator.getConfig().getValue("hotel.notifica.min_rank", "2"));
        return habbo.getHabboInfo().getRank() != null && habbo.getHabboInfo().getRank().getId() >= minRank;
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        Habbo habbo = gameClient.getHabbo();

        if (!hasStaffRank(habbo)) {
            habbo.whisper(Emulator.getTexts().getValue("notifica.error.rank", "Non hai i permessi per usare questo comando."), RoomChatMessageBubbles.ALERT);
            return true;
        }

        Room room = habbo.getHabboInfo().getCurrentRoom();
        if (room == null) {
            habbo.whisper(Emulator.getTexts().getValue("notifica.error.room", "Devi trovarti nella stanza da annunciare."), RoomChatMessageBubbles.ALERT);
            return true;
        }

        String type = params.length >= 2 ? params[1].toLowerCase(Locale.ROOT) : "";
        boolean known = false;
        for (String candidate : TYPES) {
            if (candidate.equals(type)) known = true;
        }

        if (!known) {
            habbo.whisper(Emulator.getTexts().getValue("notifica.error.usage", "Uso: :notifica <arb|staff|pok|evento> [testo]"), RoomChatMessageBubbles.ALERT);
            return true;
        }

        String extra = params.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(params, 2, params.length)) : "";
        send(habbo, room, type, extra);
        habbo.whisper(Emulator.getTexts().getValue("notifica.sent", "Notifica inviata all'hotel."), RoomChatMessageBubbles.ALERT);
        return true;
    }

    static void send(Habbo sender, Room room, String type, String extra) {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("title", text(type, "title", defaultTitle(type)));
        parameters.put("message", text(type, "message", defaultMessage(type)));
        parameters.put("image", text(type, "image", ""));
        parameters.put("linkTitle", text(type, "link_title", "Vai alla stanza"));
        // OpenUrl() feeds this straight into CreateLinkEvent, so no "event:" prefix here.
        parameters.put("linkUrl", "navigator/goto/" + room.getId());
        parameters.put("display", text(type, "display", "ALERT"));

        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            entry.setValue(replace(entry.getValue(), sender, room, extra));
        }

        BubbleAlertComposer composer = new BubbleAlertComposer("hotel.notifica." + type, parameters);
        for (Habbo online : Emulator.getGameEnvironment().getHabboManager().getOnlineHabbos().values()) {
            if (online.getClient() != null) online.getClient().sendResponse(composer);
        }
    }

    private static String text(String type, String field, String fallback) {
        return Emulator.getTexts().getValue("notifica." + type + "." + field, fallback);
    }

    private static String replace(String value, Habbo sender, Room room, String extra) {
        if (value == null) return "";
        return value.replace("%user%", sender.getHabboInfo().getUsername())
                .replace("%owner%", room.getOwnerName() == null ? "" : room.getOwnerName())
                .replace("%room%", room.getName() == null ? "" : room.getName())
                .replace("%roomid%", String.valueOf(room.getId()))
                .replace("%desc%", room.getDescription() == null ? "" : room.getDescription())
                .replace("%extra%", extra == null ? "" : extra);
    }

    private static String defaultTitle(String type) {
        switch (type) {
            case "arb":
                return "★ Evento Arbitri ★";
            case "staff":
                return "★ Evento Staff ★";
            case "pok":
                return "♠ Poker aperto ♠";
            default:
                return "★ Evento ★";
        }
    }

    private static String defaultMessage(String type) {
        String room = "\r%room%\r%desc%\r\r";
        switch (type) {
            case "arb":
                return "Un Arbitro ha aperto un evento!" + room + "Vieni qui per vincere un raro v10 o premi!\r%extra%";
            case "staff":
                return "Lo Staff ha aperto un evento!" + room + "Vieni qui per vincere un raro v10 o premi!\r%extra%";
            case "pok":
                return "%user% ha aperto un poker!" + room + "Clicca qui per raggiungere il tavolo.\r%extra%";
            default:
                return "%user% ha aperto un evento!" + room + "Vieni qui per vincere un raro v10 o premi!\r%extra%";
        }
    }
}

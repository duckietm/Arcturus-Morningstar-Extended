package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.generic.alerts.StaffAlertWithLinkComposer;
import java.util.Locale;
import java.util.Set;

/** BSS event announcements whose labels remain editable in emulator settings/housekeeping. */
final class BssNotificationCommand extends Command {
    private static final Set<String> TYPES = Set.of("arb", "staff", "pok", "evento");

    BssNotificationCommand() {
        super("cmd_bss_notification", Emulator.getTexts().getValue("commands.keys.cmd_bss_notification").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        if (params.length != 2) return usage(gameClient);
        return announce(gameClient, params[1]);
    }

    static boolean announce(GameClient gameClient, String requestedType) {
        Room room = gameClient.getHabbo().getHabboInfo().getCurrentRoom();
        String type = requestedType == null ? "" : requestedType.toLowerCase(Locale.ROOT);
        if (room == null || !TYPES.contains(type)) return usage(gameClient);

        String headline = Emulator.getConfig().getValue(
                "bss.notification." + type + ".headline",
                switch (type) {
                    case "arb" -> "ARBITRI RICHIESTI";
                    case "staff" -> "STAFF RICHIESTO";
                    case "pok" -> "ZONA POKER APERTA";
                    default -> "EVENTO IN CORSO";
                });
        String body = Emulator.getConfig().getValue(
                "bss.notification." + type + ".text",
                "pok".equals(type)
                        ? "Ora e aperto un poker da %user%. Clicca qui per andare."
                        : "Vieni qui per vincere un raro v10 o altri premi!");
        String description = room.getDescription() == null || room.getDescription().isBlank()
                ? "Nessuna descrizione"
                : room.getDescription();
        String message = "[ " + headline + " ]\r\n"
                + body.replace("%user%", gameClient.getHabbo().getHabboInfo().getUsername())
                        .replace("%room%", room.getName())
                        .replace("%description%", description)
                + "\r\n\r\nStanza: " + room.getName()
                + "\r\nDescrizione: " + description
                + "\r\n\r\nClicca qui per entrare!";
        String link = "navigator/goto/" + room.getId();
        ServerMessage alert = new StaffAlertWithLinkComposer(message, link).compose();

        for (Habbo recipient : Emulator.getGameEnvironment().getHabboManager().getOnlineHabbos().values()) {
            if (!recipient.getHabboStats().blockStaffAlerts && recipient.getClient() != null) {
                recipient.getClient().sendResponse(alert);
            }
        }

        gameClient.getHabbo().whisper(
                Emulator.getTexts().getValue("commands.success.cmd_bss_notification")
                        .replace("%type%", type)
                        .replace("%room%", room.getName()),
                RoomChatMessageBubbles.ALERT);
        return true;
    }

    private static boolean usage(GameClient gameClient) {
        gameClient.getHabbo().whisper(
                Emulator.getTexts().getValue("commands.error.cmd_bss_notification.usage"),
                RoomChatMessageBubbles.ALERT);
        return true;
    }
}

final class BssOpenPokerCommand extends Command {
    BssOpenPokerCommand() {
        super("cmd_bss_open_poker", Emulator.getTexts().getValue("commands.keys.cmd_bss_open_poker").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        return BssNotificationCommand.announce(gameClient, "pok");
    }
}

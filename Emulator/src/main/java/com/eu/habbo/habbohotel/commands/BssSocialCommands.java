package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.modtool.ModToolIssue;
import com.eu.habbo.habbohotel.modtool.ModToolTicketType;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.UserPrefix;
import com.eu.habbo.messages.outgoing.inventory.prefixes.UserPrefixesComposer;
import com.eu.habbo.messages.outgoing.modtool.ModToolReportReceivedAlertComposer;
import com.eu.habbo.messages.outgoing.modtool.ReportRoomFormComposer;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserTagsComposer;
import com.eu.habbo.messages.outgoing.users.UserPerksComposer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BssTagCommand extends Command {
    enum Operation {
        ADD,
        REMOVE,
        CLEAR
    }

    private final Operation operation;

    BssTagCommand(String permission, Operation operation) {
        super(permission, Emulator.getTexts().getValue("commands.keys." + permission).split(";"));
        this.operation = operation;
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        Habbo habbo = gameClient.getHabbo();
        boolean changed;
        switch (operation) {
            case ADD -> {
                if (params.length != 2 || params[1].length() > 20 || habbo.getHabboStats().tags.length >= 20) {
                    return usage(habbo);
                }
                changed = habbo.getHabboStats().addTag(params[1]);
            }
            case REMOVE -> {
                if (params.length != 2) return usage(habbo);
                changed = habbo.getHabboStats().removeTag(params[1]);
            }
            case CLEAR -> {
                changed = false;
                for (String tag : List.copyOf(Arrays.asList(habbo.getHabboStats().tags))) {
                    changed |= habbo.getHabboStats().removeTag(tag);
                }
            }
            default -> throw new IllegalStateException("Unknown tag operation");
        }

        Room room = habbo.getHabboInfo().getCurrentRoom();
        if (changed && room != null) room.sendComposer(new RoomUserTagsComposer(habbo).compose());
        habbo.whisper(
                Emulator.getTexts().getValue("commands.success." + permission + "." + (changed ? "changed" : "unchanged")),
                RoomChatMessageBubbles.ALERT);
        return true;
    }

    private boolean usage(Habbo habbo) {
        habbo.whisper(Emulator.getTexts().getValue("commands.error." + permission + ".usage"), RoomChatMessageBubbles.ALERT);
        return true;
    }
}

final class BssPersonalTradeCommand extends Command {
    BssPersonalTradeCommand() {
        super("cmd_bss_trade", Emulator.getTexts().getValue("commands.keys.cmd_bss_trade").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        Habbo habbo = gameClient.getHabbo();
        boolean enabled = !habbo.getHabboStats().allowTrade();
        habbo.getHabboStats().setAllowTrade(enabled);
        Emulator.getThreading().run(habbo.getHabboStats());
        gameClient.sendResponse(new UserPerksComposer(habbo));
        habbo.whisper(
                Emulator.getTexts().getValue("commands.success.cmd_bss_trade." + (enabled ? "enabled" : "disabled")),
                RoomChatMessageBubbles.ALERT);
        return true;
    }
}

final class BssResetPrefixCommand extends Command {
    BssResetPrefixCommand() {
        super("cmd_bss_reset_prefix", Emulator.getTexts().getValue("commands.keys.cmd_bss_reset_prefix").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        Habbo habbo = gameClient.getHabbo();
        UserPrefix active = habbo.getInventory().getPrefixesComponent().getActivePrefix();
        habbo.getInventory().getPrefixesComponent().deactivateAll();
        gameClient.sendResponse(new UserPrefixesComposer(habbo));
        habbo.whisper(
                Emulator.getTexts().getValue(active == null
                        ? "commands.success.cmd_bss_reset_prefix.unchanged"
                        : "commands.success.cmd_bss_reset_prefix.changed"),
                RoomChatMessageBubbles.ALERT);
        return true;
    }
}

final class BssReportCommand extends Command {
    BssReportCommand() {
        super("cmd_bss_report", Emulator.getTexts().getValue("commands.keys.cmd_bss_report").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        Room room = gameClient.getHabbo().getHabboInfo().getCurrentRoom();
        if (room == null || params.length < 3) return usage(gameClient);

        Habbo reported = room.getHabbo(params[1]);
        if (reported == null) {
            gameClient.getHabbo().whisper(
                    Emulator.getTexts().getValue("commands.error.target_not_found").replace("%user%", params[1]),
                    RoomChatMessageBubbles.ALERT);
            return true;
        }

        List<ModToolIssue> pendingIssues = Emulator.getGameEnvironment()
                .getModToolManager()
                .openTicketsForHabbo(gameClient.getHabbo());
        if (!pendingIssues.isEmpty()) {
            gameClient.getHabbo().whisper(
                    Emulator.getTexts().getValue("commands.error.cmd_bss_report.pending"),
                    RoomChatMessageBubbles.ALERT);
            gameClient.sendResponse(new ReportRoomFormComposer(pendingIssues));
            return true;
        }

        String reason = String.join(" ", Arrays.copyOfRange(params, 2, params.length)).strip();
        if (reason.isEmpty() || reason.length() > 255) return usage(gameClient);

        ModToolIssue issue = new ModToolIssue(
                gameClient.getHabbo().getHabboInfo().getId(),
                gameClient.getHabbo().getHabboInfo().getUsername(),
                reported.getHabboInfo().getId(),
                reported.getHabboInfo().getUsername(),
                room.getId(),
                reason,
                ModToolTicketType.NORMAL);
        Emulator.getGameEnvironment().getModToolManager().addTicket(issue);
        Emulator.getGameEnvironment().getModToolManager().updateTicketToMods(issue);
        String confirmation = Emulator.getTexts()
                .getValue("commands.success.cmd_bss_report")
                .replace("%user%", reported.getHabboInfo().getUsername());
        gameClient.sendResponse(new ModToolReportReceivedAlertComposer(
                ModToolReportReceivedAlertComposer.REPORT_RECEIVED,
                confirmation));
        gameClient.getHabbo().whisper(confirmation, RoomChatMessageBubbles.ALERT);
        return true;
    }

    private boolean usage(GameClient gameClient) {
        gameClient.getHabbo().whisper(
                Emulator.getTexts().getValue("commands.error.cmd_bss_report.usage"),
                RoomChatMessageBubbles.ALERT);
        return true;
    }
}

final class BssClearGroupChatCommand extends Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(BssClearGroupChatCommand.class);

    BssClearGroupChatCommand() {
        super("cmd_bss_clear_group_chat", Emulator.getTexts().getValue("commands.keys.cmd_bss_clear_group_chat").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        int cleared = 0;
        String sql = "UPDATE messenger_members member "
                + "JOIN messenger_conversations conversation ON conversation.id = member.conversation_id "
                + "SET member.joined_message_id = COALESCE((SELECT MAX(message.id) + 1 FROM messenger_messages message "
                + "WHERE message.conversation_id = member.conversation_id), 1), member.left_message_id = NULL "
                + "WHERE member.user_id = ? AND member.left_at IS NULL AND conversation.type = 'group'";
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, gameClient.getHabbo().getHabboInfo().getId());
            cleared = statement.executeUpdate();
        } catch (SQLException exception) {
            LOGGER.error("Failed to clear group chat history", exception);
        }
        gameClient.getHabbo().whisper(
                Emulator.getTexts().getValue("commands.success.cmd_bss_clear_group_chat")
                        .replace("%count%", Integer.toString(cleared)),
                RoomChatMessageBubbles.ALERT);
        return true;
    }
}

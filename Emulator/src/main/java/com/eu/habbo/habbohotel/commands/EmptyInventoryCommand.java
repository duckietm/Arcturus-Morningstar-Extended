package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.invsee.InvseeService;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.outgoing.inventory.InventoryRefreshComposer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Empties an inventory (the hand, i.e. {@code items.room_id = 0}; furniture placed in rooms is left alone).
 *
 * <pre>
 *   :pulisci                     -&gt; asks to confirm for your own inventory
 *   :pulisci si                  -&gt; empties your own
 *   :pulisci &lt;utente&gt;            -&gt; staff: shows how much would go and asks to confirm
 *   :pulisci &lt;utente&gt; si         -&gt; staff: empties that inventory (works offline too)
 *   :pulisci si &lt;utente&gt;         -&gt; the historical argument order, still accepted
 * </pre>
 *
 * Everything deleted is copied into {@code items_staff_purge_backup} first and the purge id is reported in the chat,
 * so a wrong wipe can be put back (the restore statement is in the migration that creates the table).
 */
public class EmptyInventoryCommand extends Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmptyInventoryCommand.class);

    private static final String COLUMNS =
            "id, user_id, room_id, item_id, wall_pos, x, y, z, rot, extra_data, wired_data, limited_data, guild_id";
    private static final String BACKUP_SQL = "INSERT INTO items_staff_purge_backup (purge_id, purged_by, " + COLUMNS
            + ") SELECT ?, ?, " + COLUMNS + " FROM items WHERE user_id = ? AND room_id = 0";
    private static final String DELETE_SQL = "DELETE FROM items WHERE user_id = ? AND room_id = 0";
    private static final String COUNT_SQL = "SELECT COUNT(*) AS total FROM items WHERE user_id = ? AND room_id = 0";

    public EmptyInventoryCommand() {
        super(
                "cmd_empty",
                Emulator.getTexts().getValue("commands.keys.cmd_empty").split(";"));
    }

    // Confirmation words accepted next to the configured generic.yes, so every alias of the
    // command (":empty yes", ":pulisci si") works without the hint pointing at a different alias.
    private static final String[] CONFIRM_WORDS = {"yes", "y", "si", "sì", "ok", "conferma"};

    static boolean isConfirmation(String word) {
        if (word == null) return false;
        if (word.equalsIgnoreCase(Emulator.getTexts().getValue("generic.yes"))) return true;
        for (String candidate : CONFIRM_WORDS) {
            if (candidate.equalsIgnoreCase(word)) return true;
        }
        return false;
    }

    private static String alias(String[] params) {
        return params.length > 0 && params[0] != null && !params[0].isEmpty() ? params[0] : "empty";
    }

    private static String verifyMessage(String[] params) {
        return Emulator.getTexts()
                .getValue("commands.succes.cmd_empty.verify")
                .replace("%generic.yes%", Emulator.getTexts().getValue("generic.yes"))
                .replace("%command%", alias(params))
                .replace(":empty ", ":" + alias(params) + " ");
    }

    private static void tell(GameClient gameClient, String message) {
        gameClient.getHabbo().whisper(message, RoomChatMessageBubbles.ALERT);
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        boolean mayEmptyOthers = gameClient.getHabbo().hasPermission(Permission.ACC_EMPTY_OTHERS);

        // ":pulisci" with nothing else: confirm for our own inventory.
        if (params.length == 1) {
            tell(gameClient, verifyMessage(params));
            return true;
        }

        String first = params[1];
        boolean firstIsConfirmation = isConfirmation(first);

        // Historical order ":pulisci si [utente]", plus the plain ":pulisci si" for our own hand.
        if (firstIsConfirmation) {
            String username = params.length >= 3 ? params[2] : null;

            if (username != null && !mayEmptyOthers) {
                tell(gameClient, Emulator.getTexts().getValue("commands.error.cmd_empty.no_permission",
                        "Non puoi svuotare l'inventario di un altro utente."));
                return true;
            }

            if (username == null) {
                emptyOwn(gameClient);
                return true;
            }

            return emptyOther(gameClient, username, params);
        }

        // ":pulisci <utente> [si]" - the form staff actually reach for.
        if (!mayEmptyOthers) {
            tell(gameClient, verifyMessage(params));
            return true;
        }

        String username = first;
        HabboInfo target = InvseeService.resolveTarget(username);

        if (target == null) {
            tell(gameClient, Emulator.getTexts()
                    .getValue("commands.error.cmd_empty.not_found", "Utente %user% non trovato.")
                    .replace("%user%", username));
            return true;
        }

        if (params.length >= 3 && isConfirmation(params[2])) return emptyOther(gameClient, username, params);

        int total = countHand(target.getId());

        tell(gameClient, Emulator.getTexts()
                .getValue(
                        "commands.succes.cmd_empty.verify_other",
                        "Stai per eliminare %count% furni dall'inventario di %user% (i furni piazzati nelle stanze restano). Scrivi :%command% %user% %generic.yes% per confermare.")
                .replace("%count%", String.valueOf(total))
                .replace("%user%", target.getUsername())
                .replace("%command%", alias(params))
                .replace("%generic.yes%", Emulator.getTexts().getValue("generic.yes")));

        return true;
    }

    private void emptyOwn(GameClient gameClient) {
        Habbo habbo = gameClient.getHabbo();
        int removed = purge(habbo.getHabboInfo().getId(), habbo.getHabboInfo().getId());

        forgetOnlineItems(habbo);

        tell(
                gameClient,
                Emulator.getTexts()
                        .getValue("commands.succes.cmd_empty.cleared")
                        .replace("%username%", habbo.getHabboInfo().getUsername())
                        .replace("%count%", String.valueOf(removed)));
    }

    private boolean emptyOther(GameClient gameClient, String username, String[] params) {
        HabboInfo target = InvseeService.resolveTarget(username);

        if (target == null) {
            tell(gameClient, Emulator.getTexts()
                    .getValue("commands.error.cmd_empty.not_found", "Utente %user% non trovato.")
                    .replace("%user%", username));
            return true;
        }

        if (target.getId() == gameClient.getHabbo().getHabboInfo().getId()) {
            emptyOwn(gameClient);
            return true;
        }

        int removed = purge(target.getId(), gameClient.getHabbo().getHabboInfo().getId());

        Habbo online = Emulator.getGameEnvironment().getHabboManager().getHabbo(target.getId());
        if (online != null) {
            forgetOnlineItems(online);
            online.getClient().sendResponse(new InventoryRefreshComposer());
        }

        LOGGER.info(
                "{} emptied the inventory of {} ({} items)",
                gameClient.getHabbo().getHabboInfo().getUsername(),
                target.getUsername(),
                removed);

        tell(
                gameClient,
                Emulator.getTexts()
                        .getValue("commands.succes.cmd_empty.cleared")
                        .replace("%username%", target.getUsername())
                        .replace("%count%", String.valueOf(removed)));

        return true;
    }

    /** Drops the hand items from an online user's cached inventory; placed furniture is untouched. */
    private void forgetOnlineItems(Habbo habbo) {
        List<HabboItem> hand = new ArrayList<>();

        for (HabboItem item : habbo.getInventory().getItemsComponent().getItems().values()) {
            if (item != null && item.getRoomId() == 0) hand.add(item);
        }

        for (HabboItem item : hand) habbo.getInventory().getItemsComponent().removeHabboItem(item);

        habbo.getClient().sendResponse(new InventoryRefreshComposer());
    }

    private int countHand(int userId) {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                PreparedStatement statement = connection.prepareStatement(COUNT_SQL)) {
            statement.setInt(1, userId);

            try (ResultSet set = statement.executeQuery()) {
                if (set.next()) return set.getInt("total");
            }
        } catch (SQLException exception) {
            LOGGER.error("Could not count the inventory of user {}", userId, exception);
        }

        return 0;
    }

    /** Copies the hand into items_staff_purge_backup, then deletes it. Returns how many rows went. */
    private int purge(int userId, int actorId) {
        long purgeId = System.currentTimeMillis();

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            connection.setAutoCommit(false);

            try {
                try (PreparedStatement backup = connection.prepareStatement(BACKUP_SQL)) {
                    backup.setLong(1, purgeId);
                    backup.setInt(2, actorId);
                    backup.setInt(3, userId);
                    backup.executeUpdate();
                }

                int removed;

                try (PreparedStatement delete = connection.prepareStatement(DELETE_SQL)) {
                    delete.setInt(1, userId);
                    removed = delete.executeUpdate();
                }

                connection.commit();
                LOGGER.info("Inventory purge {} of user {} by {}: {} items backed up and deleted", purgeId, userId, actorId, removed);

                return removed;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            LOGGER.error("Could not empty the inventory of user {}", userId, exception);
            return 0;
        }
    }
}

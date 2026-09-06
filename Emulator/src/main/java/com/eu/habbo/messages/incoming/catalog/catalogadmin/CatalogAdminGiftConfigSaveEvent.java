package com.eu.habbo.messages.incoming.catalog.catalogadmin;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.catalog.GiftConfigurationComposer;
import com.eu.habbo.messages.outgoing.catalog.catalogadmin.CatalogAdminResultComposer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Catalog editor: saves the gift wrapping configuration in one go.
 *
 * <pre>
 * int    price          hotel.gifts.special.price (paid wrapping fee)
 * string boxTypes       comma separated box ids  -> hotel.gifts.box_types
 * string ribbonTypes    comma separated ribbon ids -> hotel.gifts.ribbon_types
 * string wrappers       comma separated items_base ids (type "wrapper" in gift_wrappers)
 * string gifts          comma separated items_base ids (type "gift", the free default boxes)
 * </pre>
 *
 * Replies with {@link CatalogAdminResultComposer} prefixed "[GIFT_CONFIG]" and pushes the fresh
 * {@link GiftConfigurationComposer} to every online client, so open catalogs pick the change up live.
 */
public class CatalogAdminGiftConfigSaveEvent extends MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogAdminGiftConfigSaveEvent.class);
    private static final String PREFIX = "[GIFT_CONFIG] ";
    private static final int MAX_LIST = 64;

    @Override
    public void handle() throws Exception {
        try {
            this.handleGuarded();
        } catch (RuntimeException exception) {
            String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            LOGGER.warn("CatalogAdminGiftConfigSaveEvent rejected: {}", message);
            this.client.sendResponse(new CatalogAdminResultComposer(false, PREFIX + message));
        }
    }

    private void handleGuarded() throws Exception {
        if (!this.client.getHabbo().hasPermission(Permission.ACC_CATALOGFURNI)) {
            this.client.sendResponse(new CatalogAdminResultComposer(false, PREFIX + "No permission"));
            return;
        }

        int price = Math.max(0, this.packet.readInt());
        List<Integer> boxTypes = parseIds(this.packet.readString(), "box types");
        List<Integer> ribbonTypes = parseIds(this.packet.readString(), "ribbon types");
        List<Integer> wrappers = parseIds(this.packet.readString(), "wrappers");
        List<Integer> gifts = parseIds(this.packet.readString(), "default gifts");

        if (boxTypes.isEmpty()) throw new IllegalArgumentException("At least one box type is required");
        if (ribbonTypes.isEmpty()) throw new IllegalArgumentException("At least one ribbon type is required");
        if (gifts.isEmpty()) throw new IllegalArgumentException("At least one free gift box is required");

        for (Integer itemId : wrappers) requireBaseItem(itemId);
        for (Integer itemId : gifts) requireBaseItem(itemId);

        this.saveWrappers(wrappers, gifts);

        var config = Emulator.getConfig();
        config.update("hotel.gifts.special.price", Integer.toString(price));
        config.update("hotel.gifts.box_types", join(boxTypes));
        config.update("hotel.gifts.ribbon_types", join(ribbonTypes));
        config.saveToDatabase();

        GiftConfigurationComposer.BOX_TYPES = boxTypes;
        GiftConfigurationComposer.RIBBON_TYPES = ribbonTypes;

        Emulator.getGameEnvironment().getCatalogManager().loadGiftWrappers();

        LOGGER.info(
                "Gift configuration saved by {}: price {}, boxes {}, ribbons {}, {} wrappers, {} default gifts",
                this.client.getHabbo().getHabboInfo().getUsername(),
                price,
                join(boxTypes),
                join(ribbonTypes),
                wrappers.size(),
                gifts.size());

        this.client.sendResponse(new CatalogAdminResultComposer(true, PREFIX + "Configurazione regali salvata"));

        Emulator.getGameServer().getGameClientManager().sendBroadcastResponse(new GiftConfigurationComposer());
    }

    private void saveWrappers(List<Integer> wrappers, List<Integer> gifts) throws SQLException {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM gift_wrappers");
                    PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO gift_wrappers (type, sprite_id, item_id) VALUES (?, ?, ?)")) {
                delete.executeUpdate();
                this.insertRows(insert, "wrapper", wrappers);
                this.insertRows(insert, "gift", gifts);
                insert.executeBatch();
                int retyped = this.forceGiftInteraction(connection, wrappers, gifts);
                connection.commit();

                // A wrapper whose items_base row is not interaction "gift" is delivered as a plain furni:
                // the box shows nothing on click and the recycler path swallows the opening. Reload base
                // items so the running emulator wraps new gifts with InteractionGift right away.
                if (retyped > 0) Emulator.getGameEnvironment().getItemManager().loadItems();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    /** Every configured box must be an InteractionGift; custom furni imports arrive as "default". */
    private int forceGiftInteraction(Connection connection, List<Integer> wrappers, List<Integer> gifts) throws SQLException {
        Set<Integer> baseIds = new LinkedHashSet<>();
        for (List<Integer> group : List.of(wrappers, gifts)) {
            for (Integer id : group) {
                Item item = resolveBaseItem(id);
                if (item != null) baseIds.add(item.getId());
            }
        }
        if (baseIds.isEmpty()) return 0;

        int retyped = 0;
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE items_base SET interaction_type = 'gift' WHERE id = ? AND interaction_type <> 'gift'")) {
            for (Integer baseId : baseIds) {
                update.setInt(1, baseId);
                retyped += update.executeUpdate();
            }
        }
        return retyped;
    }

    private void insertRows(PreparedStatement insert, String type, List<Integer> itemIds) throws SQLException {
        for (Integer itemId : itemIds) {
            Item item = resolveBaseItem(itemId);
            insert.setString(1, type);
            insert.setInt(2, item.getSpriteId());
            insert.setInt(3, item.getId());
            insert.addBatch();
        }
    }

    /**
     * The gift configuration packet carries sprite ids, so the editor sends those back; official furni have
     * sprite id == items_base id, custom ones do not (Majin Buu box: sprite 2100000005, row 2136000451). Accept both.
     */
    private static Item resolveBaseItem(int id) {
        var manager = Emulator.getGameEnvironment().getItemManager();
        Item item = manager.getItem(id);

        if (item != null) return item;

        for (Item candidate : manager.getItems().values()) {
            if (candidate != null && candidate.getSpriteId() == id) return candidate;
        }

        return null;
    }

    private static void requireBaseItem(int itemId) {
        if (resolveBaseItem(itemId) == null) {
            throw new IllegalArgumentException("Unknown items_base id / sprite id " + itemId);
        }
    }

    private static List<Integer> parseIds(String raw, String label) {
        Set<Integer> ids = new LinkedHashSet<>();
        if (raw == null) return new ArrayList<>();
        for (String part : raw.split("[,;]")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;
            try {
                int value = Integer.parseInt(trimmed);
                if (value < 0) throw new IllegalArgumentException("Negative id in " + label);
                ids.add(value);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid id '" + trimmed + "' in " + label);
            }
            if (ids.size() > MAX_LIST) throw new IllegalArgumentException("Too many entries in " + label);
        }
        return new ArrayList<>(ids);
    }

    private static String join(List<Integer> ids) {
        StringBuilder builder = new StringBuilder();
        for (Integer id : ids) {
            if (builder.length() > 0) builder.append(',');
            builder.append(id);
        }
        return builder.toString();
    }
}

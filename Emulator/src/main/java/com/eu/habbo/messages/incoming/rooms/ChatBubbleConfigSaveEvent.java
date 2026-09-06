package com.eu.habbo.messages.incoming.rooms;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.catalog.catalogadmin.CatalogAdminResultComposer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CUSTOM packet 10098: the in-game bubble picker lets the staff edit one chat_bubbles row (name, enabled, min/max
 * rank, flat colours) and reloads the availability rules exactly like the :updatechatbubbles command.
 *
 * Payload: int styleId, String name, boolean enabled, int minRank, int maxRank, String color, String textColor.
 * Answers with a CatalogAdminResultComposer whose message starts with [CHAT_BUBBLE_CONFIG].
 */
public class ChatBubbleConfigSaveEvent extends MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatBubbleConfigSaveEvent.class);
    private static final String PREFIX = "[CHAT_BUBBLE_CONFIG] ";
    private static final String PERMISSION = "cmd_update_chat_bubbles";
    private static final int MAX_NAME_LENGTH = 64;

    @Override
    public void handle() throws Exception {
        int styleId = this.packet.readInt();
        String name = this.packet.readString();
        boolean enabled = this.packet.readBoolean();
        int minRank = this.packet.readInt();
        int maxRank = this.packet.readInt();
        String color = normalizeHex(this.packet.readString());
        String textColor = normalizeHex(this.packet.readString());

        if (this.client.getHabbo() == null || !this.client.getHabbo().hasPermission(PERMISSION)) {
            this.reply(false, "Non hai il permesso di modificare i fumetti");
            return;
        }

        if (name == null) name = "";
        name = name.trim();
        if (name.length() > MAX_NAME_LENGTH) name = name.substring(0, MAX_NAME_LENGTH);
        if (styleId < 0 || minRank < 0 || maxRank < 0) {
            this.reply(false, "Valori non validi");
            return;
        }
        if (maxRank > 0 && maxRank < minRank) {
            this.reply(false, "Il rank massimo deve essere maggiore o uguale al minimo");
            return;
        }

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE chat_bubbles SET name = ?, enabled = ?, min_rank = ?, max_rank = ?, color = ?, text_color = ? WHERE type = ?")) {
            statement.setString(1, name);
            statement.setInt(2, enabled ? 1 : 0);
            statement.setInt(3, minRank);
            statement.setInt(4, maxRank);
            statement.setString(5, color);
            statement.setString(6, textColor);
            statement.setInt(7, styleId);

            if (statement.executeUpdate() == 0) {
                this.reply(false, "Fumetto " + styleId + " non trovato");
                return;
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to save chat bubble {}", styleId, e);
            this.reply(false, "Errore database: " + e.getMessage());
            return;
        }

        RoomChatMessageBubbles.removeDynamicBubbles();
        Emulator.getGameEnvironment().getRoomChatBubbleManager().reload();
        LOGGER.info("Chat bubble {} updated by {} (enabled={}, minRank={}, maxRank={})",
                styleId, this.client.getHabbo().getHabboInfo().getUsername(), enabled, minRank, maxRank);
        this.reply(true, "Fumetto " + styleId + " salvato");
    }

    private void reply(boolean success, String message) {
        this.client.sendResponse(new CatalogAdminResultComposer(success, PREFIX + message));
    }

    /** Keeps only #RRGGBB (uppercase, with the hash); anything else becomes an empty string = no flat colour. */
    private static String normalizeHex(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        if (trimmed.startsWith("#")) trimmed = trimmed.substring(1);
        if (!trimmed.matches("(?i)[0-9a-f]{6}")) return "";
        return "#" + trimmed.toUpperCase();
    }
}

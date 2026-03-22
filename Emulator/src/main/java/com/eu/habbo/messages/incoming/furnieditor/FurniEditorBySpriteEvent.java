package com.eu.habbo.messages.incoming.furnieditor;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.furnieditor.FurniEditorDetailComposer;
import com.eu.habbo.messages.outgoing.furnieditor.FurniEditorResultComposer;
import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FurniEditorBySpriteEvent extends MessageHandler {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(FurniEditorBySpriteEvent.class);

    @Override
    public void handle() throws Exception {
        try {
            LOGGER.info("[FurniEditorBySprite] Received packet from user {}", this.client.getHabbo().getHabboInfo().getUsername());

            if (!this.client.getHabbo().hasPermission(Permission.ACC_CATALOGFURNI)) {
                LOGGER.warn("[FurniEditorBySprite] No permission");
                this.client.sendResponse(new FurniEditorResultComposer(false, "No permission"));
                return;
            }

            int spriteId = this.packet.readInt();
            LOGGER.info("[FurniEditorBySprite] spriteId={}", spriteId);

            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
                Map<String, Object> item = null;
                int itemId = -1;

                try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM items_base WHERE sprite_id = ? LIMIT 1")) {
                    stmt.setInt(1, spriteId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            item = FurniEditorHelper.readDetailItem(rs);
                            itemId = rs.getInt("id");
                            LOGGER.info("[FurniEditorBySprite] Found item id={} name={}", itemId, item.get("item_name"));
                        }
                    }
                }

                if (item == null) {
                    LOGGER.warn("[FurniEditorBySprite] No item found for sprite {}", spriteId);
                    this.client.sendResponse(new FurniEditorResultComposer(false, "Item not found for sprite " + spriteId));
                    return;
                }

                // Usage count
                int usageCount = 0;
                try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) as cnt FROM items WHERE item_id = ?")) {
                    stmt.setInt(1, itemId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) usageCount = rs.getInt("cnt");
                    }
                }

                // Catalog references
                List<Map<String, Object>> catalogItems = new ArrayList<>();
                try (PreparedStatement stmt = connection.prepareStatement(
                        "SELECT ci.id as ci_id, ci.catalog_name, ci.cost_credits, ci.cost_points, ci.points_type, ci.page_id, cp.caption as page_caption " +
                        "FROM catalog_items ci LEFT JOIN catalog_pages cp ON ci.page_id = cp.id " +
                        "WHERE ci.item_ids = ?")) {
                    stmt.setString(1, String.valueOf(itemId));
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            catalogItems.add(FurniEditorHelper.readCatalogRef(rs));
                        }
                    }
                }
                LOGGER.info("[FurniEditorBySprite] catalogItems={}", catalogItems.size());

                // FurniData entry
                String furniDataJson = "";
                String classname = (String) item.get("item_name");
                if (classname != null && !classname.isEmpty()) {
                    JsonObject entry = Emulator.getGameEnvironment().getFurniDataManager().findEntry(classname);
                    if (entry != null) {
                        furniDataJson = entry.toString();
                    }
                }

                LOGGER.info("[FurniEditorBySprite] Sending detail response for item {}", itemId);
                this.client.sendResponse(new FurniEditorDetailComposer(item, usageCount, catalogItems, furniDataJson));
                LOGGER.info("[FurniEditorBySprite] Response sent OK");
            }
        } catch (Exception e) {
            LOGGER.error("[FurniEditorBySprite] ERROR: {}", e.getMessage(), e);
            this.client.sendResponse(new FurniEditorResultComposer(false, "Server error: " + e.getMessage()));
        }
    }
}

package com.eu.habbo.messages.incoming.furnieditor;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.furnieditor.FurniEditorResultComposer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class FurniEditorCreateEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        if (!this.client.getHabbo().hasPermission(Permission.ACC_CATALOGFURNI)) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "No permission"));
            return;
        }

        String fieldsJson = this.packet.readString();

        JsonObject fields;
        try {
            fields = JsonParser.parseString(fieldsJson).getAsJsonObject();
        } catch (Exception e) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "Invalid JSON"));
            return;
        }

        String itemName = fields.has("itemName") ? fields.get("itemName").getAsString() : "";
        String publicName = fields.has("publicName") ? fields.get("publicName").getAsString() : "";
        int spriteId = fields.has("spriteId") ? fields.get("spriteId").getAsInt() : 0;
        String type = fields.has("type") ? fields.get("type").getAsString() : "s";
        int width = fields.has("width") ? fields.get("width").getAsInt() : 1;
        int length = fields.has("length") ? fields.get("length").getAsInt() : 1;
        double stackHeight = fields.has("stackHeight") ? fields.get("stackHeight").getAsDouble() : 0.0;

        if (itemName.isEmpty() || publicName.isEmpty()) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "itemName and publicName are required"));
            return;
        }

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            // Get next ID
            int newId = 1;
            try (PreparedStatement stmt = connection.prepareStatement("SELECT MAX(id) as max_id FROM items_base");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) newId = rs.getInt("max_id") + 1;
            }

            // Insert
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO items_base (id, sprite_id, item_name, public_name, type, width, `length`, stack_height, " +
                    "allow_stack, allow_walk, allow_sit, allow_lay, allow_gift, allow_trade, allow_recycle, " +
                    "allow_marketplace_sell, allow_inventory_stack, interaction_type, interaction_modes_count, customparams) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setInt(1, newId);
                stmt.setInt(2, spriteId);
                stmt.setString(3, itemName);
                stmt.setString(4, publicName);
                stmt.setString(5, type);
                stmt.setInt(6, width);
                stmt.setInt(7, length);
                stmt.setDouble(8, stackHeight);
                stmt.setString(9, boolField(fields, "allowStack", "1"));
                stmt.setString(10, boolField(fields, "allowWalk", "0"));
                stmt.setString(11, boolField(fields, "allowSit", "0"));
                stmt.setString(12, boolField(fields, "allowLay", "0"));
                stmt.setString(13, boolField(fields, "allowGift", "1"));
                stmt.setString(14, boolField(fields, "allowTrade", "1"));
                stmt.setString(15, boolField(fields, "allowRecycle", "1"));
                stmt.setString(16, boolField(fields, "allowMarketplaceSell", "1"));
                stmt.setString(17, boolField(fields, "allowInventoryStack", "1"));
                stmt.setString(18, fields.has("interactionType") ? fields.get("interactionType").getAsString() : "default");
                stmt.setInt(19, fields.has("interactionModesCount") ? fields.get("interactionModesCount").getAsInt() : 1);
                stmt.setString(20, fields.has("customparams") ? fields.get("customparams").getAsString() : "");
                stmt.execute();
            }

            // Refresh item cache
            Emulator.getGameEnvironment().getItemManager().loadItems();

            this.client.sendResponse(new FurniEditorResultComposer(true, "Item created", newId));
        }
    }

    private String boolField(JsonObject fields, String key, String defaultVal) {
        if (!fields.has(key)) return defaultVal;
        return fields.get(key).getAsBoolean() ? "1" : "0";
    }
}

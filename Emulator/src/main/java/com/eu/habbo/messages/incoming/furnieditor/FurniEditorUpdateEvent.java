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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FurniEditorUpdateEvent extends MessageHandler {

    private static final Map<String, String> COL_MAP = new HashMap<>();

    static {
        COL_MAP.put("itemName", "item_name");
        COL_MAP.put("publicName", "public_name");
        COL_MAP.put("spriteId", "sprite_id");
        COL_MAP.put("type", "type");
        COL_MAP.put("width", "width");
        COL_MAP.put("length", "length");
        COL_MAP.put("stackHeight", "stack_height");
        COL_MAP.put("allowStack", "allow_stack");
        COL_MAP.put("allowWalk", "allow_walk");
        COL_MAP.put("allowSit", "allow_sit");
        COL_MAP.put("allowLay", "allow_lay");
        COL_MAP.put("allowGift", "allow_gift");
        COL_MAP.put("allowTrade", "allow_trade");
        COL_MAP.put("allowRecycle", "allow_recycle");
        COL_MAP.put("allowMarketplaceSell", "allow_marketplace_sell");
        COL_MAP.put("allowInventoryStack", "allow_inventory_stack");
        COL_MAP.put("interactionType", "interaction_type");
        COL_MAP.put("interactionModesCount", "interaction_modes_count");
        COL_MAP.put("vendingIds", "vending_ids");
        COL_MAP.put("customparams", "customparams");
        COL_MAP.put("effectIdMale", "effect_id_male");
        COL_MAP.put("effectIdFemale", "effect_id_female");
        COL_MAP.put("clothingOnWalk", "clothing_on_walk");
        COL_MAP.put("multiheight", "multiheight");
        COL_MAP.put("description", "description");
    }

    private static final List<String> BOOL_COLS = List.of(
            "allow_stack", "allow_walk", "allow_sit", "allow_lay",
            "allow_gift", "allow_trade", "allow_recycle",
            "allow_marketplace_sell", "allow_inventory_stack"
    );

    @Override
    public void handle() throws Exception {
        if (!this.client.getHabbo().hasPermission(Permission.ACC_CATALOGFURNI)) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "No permission"));
            return;
        }

        int id = this.packet.readInt();
        String fieldsJson = this.packet.readString();

        JsonObject fields;
        try {
            fields = JsonParser.parseString(fieldsJson).getAsJsonObject();
        } catch (Exception e) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "Invalid JSON"));
            return;
        }

        // Build SET clause
        List<String> setClauses = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        for (Map.Entry<String, String> entry : COL_MAP.entrySet()) {
            String jsKey = entry.getKey();
            String dbCol = entry.getValue();

            if (!fields.has(jsKey)) continue;

            setClauses.add(dbCol + " = ?");

            if (BOOL_COLS.contains(dbCol)) {
                params.add(fields.get(jsKey).getAsBoolean() ? "1" : "0");
            } else if (dbCol.equals("stack_height")) {
                params.add(fields.get(jsKey).getAsDouble());
            } else if (dbCol.equals("width") || dbCol.equals("length") || dbCol.equals("interaction_modes_count")
                    || dbCol.equals("sprite_id") || dbCol.equals("effect_id_male") || dbCol.equals("effect_id_female")) {
                params.add(fields.get(jsKey).getAsInt());
            } else {
                params.add(fields.get(jsKey).getAsString());
            }
        }

        if (setClauses.isEmpty()) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "No fields to update"));
            return;
        }

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            // Get current item_name for FurniData update
            String currentItemName = null;
            try (PreparedStatement stmt = connection.prepareStatement("SELECT item_name FROM items_base WHERE id = ?")) {
                stmt.setInt(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) currentItemName = rs.getString("item_name");
                }
            }

            if (currentItemName == null) {
                this.client.sendResponse(new FurniEditorResultComposer(false, "Item not found"));
                return;
            }

            // Execute update
            String sql = "UPDATE items_base SET " + String.join(", ", setClauses) + " WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                for (int i = 0; i < params.size(); i++) {
                    Object p = params.get(i);
                    if (p instanceof Integer) stmt.setInt(i + 1, (Integer) p);
                    else if (p instanceof Double) stmt.setDouble(i + 1, (Double) p);
                    else stmt.setString(i + 1, (String) p);
                }
                stmt.setInt(params.size() + 1, id);
                stmt.execute();
            }

            // Update FurnitureData.json if publicName changed
            if (fields.has("publicName")) {
                Emulator.getGameEnvironment().getFurniDataManager().updateEntry(currentItemName, fields.get("publicName").getAsString());
            }

            // Refresh item cache
            Emulator.getGameEnvironment().getItemManager().loadItems();

            this.client.sendResponse(new FurniEditorResultComposer(true, "Item updated"));
        }
    }
}

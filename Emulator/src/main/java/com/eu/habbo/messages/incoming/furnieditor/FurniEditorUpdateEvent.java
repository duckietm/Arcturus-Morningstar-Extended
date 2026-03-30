package com.eu.habbo.messages.incoming.furnieditor;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.furnieditor.FurniEditorResultComposer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FurniEditorUpdateEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        if (!this.client.getHabbo().hasPermission(Permission.ACC_CATALOGFURNI)) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "No permission"));
            return;
        }

        int id = this.packet.readInt();
        String jsonFieldsStr = this.packet.readString();

        if (id <= 0) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "Invalid item ID"));
            return;
        }

        JsonObject json;
        try {
            json = JsonParser.parseString(jsonFieldsStr).getAsJsonObject();
        } catch (Exception e) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "Invalid JSON data"));
            return;
        }

        if (json.size() == 0) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "No fields to update"));
            return;
        }

        // Build dynamic UPDATE with whitelisted fields
        StringBuilder setClauses = new StringBuilder();
        List<Object> values = new ArrayList<>();

        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String jsKey = entry.getKey();
            String dbColumn = FurniEditorHelper.FIELD_MAP.get(jsKey);

            if (dbColumn == null || !FurniEditorHelper.ALLOWED_UPDATE_FIELDS.contains(dbColumn)) {
                continue; // Skip unknown or disallowed fields
            }

            if (setClauses.length() > 0) setClauses.append(", ");
            setClauses.append("`").append(dbColumn).append("` = ?");

            JsonElement val = entry.getValue();
            if (val.isJsonPrimitive()) {
                if (val.getAsJsonPrimitive().isBoolean()) {
                    values.add(val.getAsBoolean() ? "1" : "0");
                } else if (val.getAsJsonPrimitive().isNumber()) {
                    // Check if it's a decimal number
                    String numStr = val.getAsString();
                    if (numStr.contains(".")) {
                        values.add(val.getAsDouble());
                    } else {
                        values.add(val.getAsInt());
                    }
                } else {
                    values.add(val.getAsString());
                }
            } else {
                values.add(val.toString());
            }
        }

        if (setClauses.length() == 0) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "No valid fields to update"));
            return;
        }

        String sql = "UPDATE items_base SET " + setClauses + " WHERE id = ?";

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            int idx = 1;
            for (Object value : values) {
                if (value instanceof Integer) {
                    stmt.setInt(idx++, (Integer) value);
                } else if (value instanceof Double) {
                    stmt.setDouble(idx++, (Double) value);
                } else {
                    stmt.setString(idx++, String.valueOf(value));
                }
            }
            stmt.setInt(idx, id);
            stmt.executeUpdate();
        }

        // Reload emulator item definitions
        Emulator.getGameEnvironment().getItemManager().loadItems();

        this.client.sendResponse(new FurniEditorResultComposer(true, "Item updated", id));
    }
}

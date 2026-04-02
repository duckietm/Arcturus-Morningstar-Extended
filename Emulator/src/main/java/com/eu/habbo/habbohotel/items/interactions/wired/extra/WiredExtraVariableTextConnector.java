package com.eu.habbo.habbohotel.items.interactions.wired.extra;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredExtra;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.messages.ServerMessage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class WiredExtraVariableTextConnector extends InteractionWiredExtra {
    public static final int CODE = 79;
    private static final int MAX_MAPPING_LENGTH = 4096;

    private String mappingsText = "";
    private LinkedHashMap<Integer, String> mappings = new LinkedHashMap<>();

    public WiredExtraVariableTextConnector(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredExtraVariableTextConnector(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return false;
    }

    @Override
    public boolean saveData(WiredSettings settings, GameClient gameClient) {
        this.setMappingsText(settings.getStringParam());
        return true;
    }

    @Override
    public String getWiredData() {
        return WiredManager.getGson().toJson(new JsonData(this.mappingsText));
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(0);
        message.appendInt(0);
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString(this.mappingsText);
        message.appendInt(0);
        message.appendInt(0);
        message.appendInt(CODE);
        message.appendInt(0);
        message.appendInt(0);
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.onPickUp();

        String wiredData = set.getString("wired_data");
        if (wiredData == null || wiredData.isEmpty()) {
            return;
        }

        if (wiredData.startsWith("{")) {
            JsonData data = WiredManager.getGson().fromJson(wiredData, JsonData.class);

            if (data != null) {
                this.setMappingsText(data.mappingsText);
            }

            return;
        }

        this.setMappingsText(wiredData);
    }

    @Override
    public void onPickUp() {
        this.mappingsText = "";
        this.mappings = new LinkedHashMap<>();
    }

    @Override
    public void onWalk(RoomUnit roomUnit, Room room, Object[] objects) {
    }

    @Override
    public boolean hasConfiguration() {
        return true;
    }

    public String getMappingsText() {
        return this.mappingsText;
    }

    public Map<Integer, String> getMappings() {
        return Collections.unmodifiableMap(this.mappings);
    }

    public String resolveText(Integer value) {
        if (value == null) {
            return "";
        }

        String mappedValue = this.mappings.get(value);
        return mappedValue != null ? mappedValue : String.valueOf(value);
    }

    public Integer resolveValue(String text) {
        if (text == null) {
            return null;
        }

        String normalizedText = text.trim();
        if (normalizedText.isEmpty()) {
            return null;
        }

        for (Map.Entry<Integer, String> entry : this.mappings.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                continue;
            }

            if (entry.getValue().trim().equalsIgnoreCase(normalizedText)) {
                return entry.getKey();
            }
        }

        return null;
    }

    private void setMappingsText(String value) {
        this.mappingsText = normalizeMappingsText(value);
        this.mappings = parseMappings(this.mappingsText);
    }

    private static String normalizeMappingsText(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.replace("\r", "");

        if (normalized.length() > MAX_MAPPING_LENGTH) {
            normalized = normalized.substring(0, MAX_MAPPING_LENGTH);
        }

        return normalized;
    }

    private static LinkedHashMap<Integer, String> parseMappings(String value) {
        LinkedHashMap<Integer, String> result = new LinkedHashMap<>();
        if (value == null || value.isEmpty()) {
            return result;
        }

        for (String rawLine : value.split("\n")) {
            if (rawLine == null) {
                continue;
            }

            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            int separatorIndex = line.indexOf('=');
            if (separatorIndex < 0) {
                separatorIndex = line.indexOf(',');
            }

            if (separatorIndex <= 0) {
                continue;
            }

            String keyPart = line.substring(0, separatorIndex).trim();
            String valuePart = line.substring(separatorIndex + 1).trim();

            try {
                result.put(Integer.parseInt(keyPart), valuePart);
            } catch (NumberFormatException ignored) {
            }
        }

        return result;
    }

    static class JsonData {
        String mappingsText;

        JsonData(String mappingsText) {
            this.mappingsText = mappingsText;
        }
    }
}

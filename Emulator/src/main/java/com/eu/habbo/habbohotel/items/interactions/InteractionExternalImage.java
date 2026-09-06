package com.eu.habbo.habbohotel.items.interactions;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.ServerMessage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

public class InteractionExternalImage extends HabboItem {
    private static final Pattern SAFE_CAMERA_FILE = Pattern.compile("[A-Za-z0-9._-]+\\.png", Pattern.CASE_INSENSITIVE);
    public InteractionExternalImage(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public InteractionExternalImage(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean canWalkOn(RoomUnit roomUnit, Room room, Object[] objects) {
        return false;
    }

    @Override
    public boolean isWalkable() {
        return false;
    }

    @Override
    public void onWalk(RoomUnit roomUnit, Room room, Object[] objects) throws Exception {

    }

    @Override
    public void serializeExtradata(ServerMessage serverMessage) {
        serverMessage.appendInt((this.isLimited() ? 256 : 0));
        serverMessage.appendString(normalizePhotoExtradata(this.getExtradata()));

        super.serializeExtradata(serverMessage);
    }

    @Override
    public void onClick(GameClient client, Room room, Object[] objects) throws Exception {
    }

    static String normalizePhotoExtradata(String raw) {
        if (raw == null || raw.isBlank()) return raw;

        try {
            JsonObject object = JsonParser.parseString(raw).getAsJsonObject();
            if (!object.has("w") || !object.get("w").isJsonPrimitive()) return raw;

            String value = object.get("w").getAsString();
            URI uri = URI.create(value);
            String host = uri.getHost();
            if (host == null) return raw;

            String normalizedHost = host.toLowerCase(Locale.ROOT);
            if (!normalizedHost.equals("127.0.0.1")
                    && !normalizedHost.equals("localhost")
                    && !normalizedHost.equals("photo.bsshotel.it")) return raw;

            String path = uri.getPath();
            if (path == null || path.isBlank()) return raw;
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            if (!SAFE_CAMERA_FILE.matcher(fileName).matches()) return raw;

            object.addProperty("w", "/camera/" + fileName);
            return object.toString();
        } catch (RuntimeException ignored) {
            return raw;
        }
    }

    //{"t":10000000, "u":"http://arcturus.pw/camera/", "m":"idk", "s":1, "w":"http://arcturus.pw/camera/image.png"}
}

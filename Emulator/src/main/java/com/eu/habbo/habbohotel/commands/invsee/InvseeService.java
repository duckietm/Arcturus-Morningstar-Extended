package com.eu.habbo.habbohotel.commands.invsee;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.users.HabboManager;
import com.eu.habbo.messages.outgoing.invsee.InvseeInventoryComposer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Staff inventory inspection (:invsee). Reads the target's hand (items with
 * room_id = 0) straight from the database so it works for offline users too,
 * grouped by base item for a compact picker-style listing.
 */
public final class InvseeService {
    public static final String PERMISSION_KEY = "cmd_invsee";

    private static final String LIST_SQL =
            "SELECT i.item_id AS base_id, b.sprite_id, b.item_name, b.public_name, COUNT(*) AS cnt "
                    + "FROM items i JOIN items_base b ON b.id = i.item_id "
                    + "WHERE i.user_id = ? AND i.room_id = 0 "
                    + "GROUP BY i.item_id, b.sprite_id, b.item_name, b.public_name "
                    + "ORDER BY b.public_name ASC, i.item_id ASC";

    private InvseeService() {}

    public record ItemGroup(int baseItemId, int spriteId, String name, int count) {}

    public static HabboInfo resolveTarget(String username) {
        Habbo online = Emulator.getGameEnvironment().getHabboManager().getHabbo(username);
        if (online != null) return online.getHabboInfo();
        return HabboManager.getOfflineHabboInfo(username);
    }

    public static void sendInventory(GameClient client, HabboInfo target) {
        List<ItemGroup> groups = new ArrayList<>();

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(LIST_SQL)) {
            statement.setInt(1, target.getId());
            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    String publicName = set.getString("public_name");
                    String name = (publicName != null && !publicName.isBlank())
                            ? publicName
                            : set.getString("item_name");
                    groups.add(new ItemGroup(
                            set.getInt("base_id"),
                            set.getInt("sprite_id"),
                            name == null ? "" : name,
                            set.getInt("cnt")));
                }
            }
        } catch (SQLException e) {
            org.slf4j.LoggerFactory.getLogger(InvseeService.class)
                    .error("invsee: failed to list inventory of {}", target.getUsername(), e);
        }

        boolean online = Emulator.getGameEnvironment().getHabboManager().getHabbo(target.getId()) != null;
        client.sendResponse(new InvseeInventoryComposer(target.getId(), target.getUsername(), online, groups));
    }
}

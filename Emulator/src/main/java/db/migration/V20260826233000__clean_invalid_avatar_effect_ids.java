package db.migration;

import com.eu.habbo.habbohotel.rooms.AvatarEffectSupport;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Removes persisted effect references that Nitro cannot render. */
public final class V20260826233000__clean_invalid_avatar_effect_ids extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        Set<Integer> supported = AvatarEffectSupport.supportedIds();
        String placeholders = supported.stream().map(ignored -> "?").collect(Collectors.joining(","));

        clearInvalid(connection, "items_base", "effect_id_male", placeholders, supported);
        clearInvalid(connection, "items_base", "effect_id_female", placeholders, supported);
        clearInvalid(connection, "items_crackable", "required_effect", placeholders, supported);
        clearInvalid(connection, "bots", "effect", placeholders, supported);
        // Prefix effects are CSS/presentation names (for example "glow" or
        // "discord-neon"), not numeric Nitro avatar effect IDs.
        clearInvalid(connection, "permissions", "room_effect", placeholders, supported);
        clearInvalid(connection, "permission_ranks", "room_effect", placeholders, supported);

        deleteInvalid(connection, "users_effects", "effect", placeholders, supported);
        deleteInvalid(connection, "special_enables", "effect_id", placeholders, supported);
    }

    private static void clearInvalid(
            Connection connection,
            String table,
            String column,
            String placeholders,
            Set<Integer> supported) throws SQLException {
        if (!hasColumn(connection, table, column)) return;
        execute(connection,
                "UPDATE `" + table + "` SET `" + column + "`=0 WHERE `" + column
                        + "`<>0 AND `" + column + "` NOT IN (" + placeholders + ")",
                supported);
    }

    private static void deleteInvalid(
            Connection connection,
            String table,
            String column,
            String placeholders,
            Set<Integer> supported) throws SQLException {
        if (!hasColumn(connection, table, column)) return;
        execute(connection,
                "DELETE FROM `" + table + "` WHERE `" + column + "`<>0 AND `" + column
                        + "` NOT IN (" + placeholders + ")",
                supported);
    }

    private static void execute(
            Connection connection, String sql, Set<Integer> supported) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (int id : supported) statement.setInt(index++, id);
            statement.executeUpdate();
        }
    }

    private static boolean hasColumn(Connection connection, String table, String column) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet result = metadata.getColumns(connection.getCatalog(), null, table, column)) {
            return result.next();
        }
    }
}

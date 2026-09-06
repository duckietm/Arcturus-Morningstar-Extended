package com.eu.habbo.habbohotel.rooms;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Persists the hotel home room and keeps every connected user in sync. */
public final class HotelHomeRoomService {
    private HotelHomeRoomService() {}

    public static int setForEveryone(int roomId) throws SQLException {
        int updatedUsers;

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            connection.setAutoCommit(false);

            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO emulator_settings (`key`, `value`) VALUES ('hotel.home.room', ?) "
                                + "ON DUPLICATE KEY UPDATE `value` = VALUES(`value`)")) {
                    statement.setString(1, Integer.toString(roomId));
                    statement.executeUpdate();
                }

                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO website_settings (`key`, `value`, `comment`) VALUES "
                                + "('hotel_home_room', ?, 'The default home room for every new user') "
                                + "ON DUPLICATE KEY UPDATE `value` = VALUES(`value`)")) {
                    statement.setString(1, Integer.toString(roomId));
                    statement.executeUpdate();
                }

                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE users SET home_room = ?")) {
                    statement.setInt(1, roomId);
                    updatedUsers = statement.executeUpdate();
                }

                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        }

        RoomManager.HOME_ROOM_ID = roomId;
        for (Habbo habbo : Emulator.getGameEnvironment().getHabboManager().getOnlineHabbos().values()) {
            if (habbo != null) {
                habbo.getHabboInfo().setHomeRoom(roomId);
            }
        }

        return updatedUsers;
    }
}
